use once_cell::sync::Lazy;
use std::any::Any;
use std::cell::RefCell;
use std::collections::{HashMap, HashSet};
use std::mem;
use std::sync::{Arc, Mutex};
use std::sync::atomic::{AtomicU32, Ordering};

pub type SnapshotId = u32;

#[derive(Clone, Default, Debug)]
pub struct SnapshotIdSet {
    inner: HashSet<SnapshotId>,
}

impl SnapshotIdSet {
    #[inline]
    pub fn set(&self, id: SnapshotId) -> Self {
        let mut cloned = self.clone();
        cloned.inner.insert(id);
        cloned
    }

    #[inline]
    pub fn clear(&self, id: SnapshotId) -> Self {
        let mut cloned = self.clone();
        cloned.inner.remove(&id);
        cloned
    }

    #[inline]
    pub fn get(&self, id: SnapshotId) -> bool {
        self.inner.contains(&id)
    }

    #[inline]
    pub fn or(&self, other: SnapshotIdSet) -> SnapshotIdSet {
        let mut cloned = self.clone();
        cloned.inner.extend(other.inner);
        cloned
    }

    #[inline]
    pub fn add_range(&self, from: SnapshotId, until: SnapshotId) -> SnapshotIdSet {
        let mut cloned = self.clone();
        for id in from..until {
            cloned.inner.insert(id);
        }
        cloned
    }
}

pub trait StateObject: Any + Send + Sync {
    fn as_any(&self) -> &dyn Any;

    fn read_current(&self) -> Box<dyn Any + Send + Sync>;

    fn apply(&self, value: &dyn Any);

    fn equals(&self, a: &dyn Any, b: &dyn Any) -> bool;

    fn clone_value(&self, value: &dyn Any) -> Box<dyn Any + Send + Sync>;

    fn merge_records(
        &self,
        _previous: &dyn Any,
        _current: &dyn Any,
        applied: &dyn Any,
    ) -> Option<Box<dyn Any + Send + Sync>> {
        Some(self.clone_value(applied))
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub(crate) struct StateRef {
    data: usize,
    vtable: usize,
}

unsafe impl Send for StateRef {}
unsafe impl Sync for StateRef {}

impl StateRef {
    pub(crate) fn new(object: &dyn StateObject) -> Self {
        let ptr = object as *const dyn StateObject;
        let raw: [usize; 2] = unsafe { mem::transmute(ptr) };
        StateRef {
            data: raw[0],
            vtable: raw[1],
        }
    }

    pub(crate) fn object(&self) -> &'static dyn StateObject {
        let raw = [self.data, self.vtable];
        let ptr: *const dyn StateObject = unsafe { mem::transmute(raw) };
        unsafe { &*ptr }
    }
}

#[derive(Clone)]
pub struct Snapshot(Arc<SnapshotInner>);

impl Snapshot {
    fn new(inner: SnapshotInner) -> Self {
        Snapshot(Arc::new(inner))
    }

    pub fn id(&self) -> SnapshotId {
        self.0.id
    }

    pub fn is_read_only(&self) -> bool {
        self.0.read_only
    }

    pub(crate) fn modified(&self) -> Option<&Mutex<HashMap<StateRef, Box<dyn Any + Send + Sync>>>> {
        if self.0.is_global {
            None
        } else {
            self.0.modified.as_ref()
        }
    }

    pub fn parent(&self) -> Option<Snapshot> {
        self.0.parent.as_ref().map(|arc| Snapshot(arc.clone()))
    }

    pub(crate) fn notify_read(&self, target: &dyn Any) {
        self.0.notify_read(target);
    }

    pub(crate) fn notify_write(&self, target: &dyn Any) {
        self.0.notify_write(target);
    }

    pub(crate) fn lookup_cloned<T: Clone + 'static>(&self, key: StateRef) -> Option<T> {
        self.0.lookup_cloned::<T>(key)
    }

    pub(crate) fn store_previous<T: Clone + Send + Sync + 'static>(&self, key: StateRef, value: &T) {
        if !self.0.is_global {
            self.0.store_previous::<T>(key, value);
        }
    }

    fn parent_arc(&self) -> Option<Arc<SnapshotInner>> {
        self.0.parent.clone()
    }

    fn inner(&self) -> &Arc<SnapshotInner> {
        &self.0
    }
}

pub struct MutableSnapshot(pub Snapshot);

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SnapshotApplyResult {
    Success,
    Failure,
}

pub struct ObserverHandle(Box<dyn FnOnce()>);

impl ObserverHandle {
    pub fn new(f: impl FnOnce() + 'static) -> Self {
        ObserverHandle(Box::new(f))
    }

    pub fn dispose(self) {
        (self.0)();
    }
}

struct SnapshotInner {
    id: SnapshotId,
    _invalid: SnapshotIdSet,
    read_only: bool,
    read_observer: Option<Arc<dyn Fn(&dyn Any) + Send + Sync>>,
    write_observer: Option<Arc<dyn Fn(&dyn Any) + Send + Sync>>,
    modified: Option<Mutex<HashMap<StateRef, Box<dyn Any + Send + Sync>>>>,
    previous: Mutex<HashMap<StateRef, Box<dyn Any + Send + Sync>>>,
    parent: Option<Arc<SnapshotInner>>,
    is_global: bool,
}

impl SnapshotInner {
    fn new_root() -> Self {
        SnapshotInner {
            id: 0,
            _invalid: SnapshotIdSet::default(),
            read_only: false,
            read_observer: None,
            write_observer: None,
            modified: Some(Mutex::new(HashMap::new())),
            previous: Mutex::new(HashMap::new()),
            parent: None,
            is_global: true,
        }
    }

    fn new_child(
        id: SnapshotId,
        read_only: bool,
        parent: Arc<SnapshotInner>,
        read_observer: Option<Arc<dyn Fn(&dyn Any) + Send + Sync>>,
        write_observer: Option<Arc<dyn Fn(&dyn Any) + Send + Sync>>,
    ) -> Self {
        SnapshotInner {
            id,
            _invalid: SnapshotIdSet::default(),
            read_only,
            read_observer,
            write_observer,
            modified: if read_only {
                None
            } else {
                Some(Mutex::new(HashMap::new()))
            },
            previous: Mutex::new(HashMap::new()),
            parent: Some(parent),
            is_global: false,
        }
    }

    fn lookup_cloned<T: Clone + 'static>(&self, key: StateRef) -> Option<T> {
        if let Some(modified) = &self.modified {
            if let Some(value) = modified.lock().unwrap().get(&key) {
                if let Some(value) = value.as_ref().downcast_ref::<T>() {
                    return Some(value.clone());
                }
            }
        }
        if let Some(parent) = &self.parent {
            return parent.lookup_cloned::<T>(key);
        }
        None
    }

    fn store_previous<T: Clone + Send + Sync + 'static>(&self, key: StateRef, value: &T) {
        let mut previous = self.previous.lock().unwrap();
        previous.entry(key).or_insert_with(|| Box::new(value.clone()));
    }

    fn notify_read(&self, target: &dyn Any) {
        if let Some(observer) = &self.read_observer {
            observer(target);
        }
        if let Some(parent) = &self.parent {
            parent.notify_read(target);
        }
    }

    fn notify_write(&self, target: &dyn Any) {
        if let Some(observer) = &self.write_observer {
            observer(target);
        }
        if let Some(parent) = &self.parent {
            parent.notify_write(target);
        }
    }

    fn is_mutable(&self) -> bool {
        self.modified.is_some()
    }

    fn is_global(&self) -> bool {
        self.is_global
    }
}

thread_local! {
    static THREAD_SNAPSHOT: RefCell<Option<Arc<SnapshotInner>>> = RefCell::new(None);
}

static GLOBAL_SNAPSHOT: Lazy<Arc<SnapshotInner>> = Lazy::new(|| Arc::new(SnapshotInner::new_root()));
static NEXT_SNAPSHOT_ID: AtomicU32 = AtomicU32::new(1);

struct SnapshotGuard {
    previous: Option<Arc<SnapshotInner>>,
}

impl SnapshotGuard {
    fn enter(snapshot: Arc<SnapshotInner>) -> Self {
        let previous = THREAD_SNAPSHOT.with(|tls| {
            let mut slot = tls.borrow_mut();
            slot.replace(snapshot)
        });
        SnapshotGuard { previous }
    }
}

impl Drop for SnapshotGuard {
    fn drop(&mut self) {
        THREAD_SNAPSHOT.with(|tls| {
            *tls.borrow_mut() = self.previous.clone();
        });
    }
}

pub fn current_snapshot() -> Snapshot {
    THREAD_SNAPSHOT.with(|tls| match &*tls.borrow() {
        Some(snapshot) => Snapshot(snapshot.clone()),
        None => Snapshot(GLOBAL_SNAPSHOT.clone()),
    })
}

fn apply_to_parent(
    snapshot: &Arc<SnapshotInner>,
    parent: &Arc<SnapshotInner>,
) -> SnapshotApplyResult {
    let Some(modified) = &snapshot.modified else {
        return SnapshotApplyResult::Success;
    };

    let mut modified = modified.lock().unwrap();
    if modified.is_empty() {
        return SnapshotApplyResult::Success;
    }

    let mut previous = snapshot.previous.lock().unwrap();

    if parent.is_mutable() && !parent.is_global() {
        let parent_modified = parent.modified.as_ref().expect("mutable parent must have storage");
        let mut parent_modified = parent_modified.lock().unwrap();
        let mut parent_previous = parent.previous.lock().unwrap();
        for (key, value) in modified.drain() {
            if !parent_previous.contains_key(&key) {
                if let Some(prev) = previous.remove(&key) {
                    parent_previous.insert(key, prev);
                } else {
                    let object = key.object();
                    parent_previous.insert(key, object.read_current());
                }
            }
            parent_modified.insert(key, value);
        }
        SnapshotApplyResult::Success
    } else {
        // parent is read-only/global; apply to real objects
        let mut pending: Vec<(StateRef, Box<dyn Any + Send + Sync>)> = Vec::new();
        for (key, value) in modified.drain() {
            let object = key.object();
            let current_value = object.read_current();
            let previous_value = previous
                .remove(&key)
                .unwrap_or_else(|| object.read_current());
            let applied_value = value;
            if !object.equals(current_value.as_ref(), previous_value.as_ref()) {
                match object
                    .merge_records(previous_value.as_ref(), current_value.as_ref(), applied_value.as_ref())
                {
                    Some(merged) => pending.push((key, merged)),
                    None => {
                        return SnapshotApplyResult::Failure;
                    }
                }
            } else {
                pending.push((key, applied_value));
            }
        }

        for (key, value) in pending {
            let object = key.object();
            object.apply(value.as_ref());
            parent.notify_write(object.as_any());
        }

        SnapshotApplyResult::Success
    }
}

fn apply_snapshot(snapshot: &Snapshot) -> SnapshotApplyResult {
    if let Some(parent) = snapshot.parent_arc() {
        apply_to_parent(snapshot.inner(), &parent)
    } else {
        SnapshotApplyResult::Success
    }
}

pub fn with_mutable_snapshot<R>(
    f: impl FnOnce() -> R,
) -> Result<R, SnapshotApplyResult> {
    let parent = current_snapshot();
    let id = NEXT_SNAPSHOT_ID.fetch_add(1, Ordering::Relaxed);
    let child = Snapshot::new(SnapshotInner::new_child(
        id,
        false,
        parent.inner().clone(),
        None,
        None,
    ));
    let guard = SnapshotGuard::enter(child.inner().clone());
    let result = f();
    drop(guard);
    match apply_snapshot(&child) {
        SnapshotApplyResult::Success => Ok(result),
        SnapshotApplyResult::Failure => Err(SnapshotApplyResult::Failure),
    }
}

pub fn observe<T>(
    read: Option<Arc<dyn Fn(&dyn Any) + Send + Sync>>,
    write: Option<Arc<dyn Fn(&dyn Any) + Send + Sync>>,
    f: impl FnOnce() -> T,
) -> T {
    let parent = current_snapshot();
    let id = NEXT_SNAPSHOT_ID.fetch_add(1, Ordering::Relaxed);
    let child = Snapshot::new(SnapshotInner::new_child(
        id,
        true,
        parent.inner().clone(),
        read,
        write,
    ));
    let guard = SnapshotGuard::enter(child.inner().clone());
    let result = f();
    drop(guard);
    result
}
