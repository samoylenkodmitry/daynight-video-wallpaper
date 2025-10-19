use std::any::Any;
use std::sync::{Arc, Mutex};

use crate::snapshots::{current_snapshot, StateObject, StateRef};

pub struct MutableState<T>
where
    T: Clone + PartialEq + Send + Sync + 'static,
{
    value: Mutex<T>,
    merge: Mutex<Option<Arc<dyn Fn(&T, &T, &T) -> Option<T> + Send + Sync>>>,
}

impl<T> MutableState<T>
where
    T: Clone + PartialEq + Send + Sync + 'static,
{
    fn state_ref(&self) -> StateRef {
        let object: &dyn StateObject = self;
        StateRef::new(object)
    }

    pub fn new(value: T) -> Self {
        MutableState {
            value: Mutex::new(value),
            merge: Mutex::new(None),
        }
    }

    pub fn with_merge(
        value: T,
        merge: impl Fn(&T, &T, &T) -> Option<T> + Send + Sync + 'static,
    ) -> Self {
        MutableState {
            value: Mutex::new(value),
            merge: Mutex::new(Some(Arc::new(merge))),
        }
    }

    pub fn set_merge(
        &self,
        merge: impl Fn(&T, &T, &T) -> Option<T> + Send + Sync + 'static,
    ) {
        *self.merge.lock().unwrap() = Some(Arc::new(merge));
    }

    pub fn clear_merge(&self) {
        *self.merge.lock().unwrap() = None;
    }

    pub fn get(&self) -> T {
        let snapshot = current_snapshot();
        let key = self.state_ref();
        if let Some(value) = snapshot.lookup_cloned::<T>(key) {
            snapshot.notify_read(self);
            return value;
        }
        let value = self.value.lock().unwrap().clone();
        snapshot.notify_read(self);
        value
    }

    pub fn set(&self, value: T) {
        let snapshot = current_snapshot();
        let key = self.state_ref();
        if snapshot.is_read_only() {
            panic!("cannot modify state within a read-only snapshot");
        }
        if let Some(modified) = snapshot.modified() {
            let mut map = modified.lock().unwrap();
            if !map.contains_key(&key) {
                let previous = snapshot
                    .parent()
                    .and_then(|parent| parent.lookup_cloned::<T>(key))
                    .unwrap_or_else(|| self.value.lock().unwrap().clone());
                snapshot.store_previous(key, &previous);
            }
            map.insert(key, Box::new(value.clone()) as Box<dyn Any + Send + Sync>);
            drop(map);
            snapshot.notify_write(self);
        } else {
            *self.value.lock().unwrap() = value.clone();
            snapshot.notify_write(self);
        }
    }

    pub fn update(&self, updater: impl FnOnce(&T) -> T) {
        let current = self.get();
        self.set(updater(&current));
    }
}

impl<T> StateObject for MutableState<T>
where
    T: Clone + PartialEq + Send + Sync + 'static,
{
    fn as_any(&self) -> &dyn Any {
        self
    }

    fn read_current(&self) -> Box<dyn Any + Send + Sync> {
        Box::new(self.value.lock().unwrap().clone())
    }

    fn apply(&self, value: &dyn Any) {
        let value = value
            .downcast_ref::<T>()
            .expect("type mismatch applying MutableState");
        *self.value.lock().unwrap() = value.clone();
    }

    fn equals(&self, a: &dyn Any, b: &dyn Any) -> bool {
        let a = a
            .downcast_ref::<T>()
            .expect("type mismatch comparing MutableState");
        let b = b
            .downcast_ref::<T>()
            .expect("type mismatch comparing MutableState");
        a == b
    }

    fn clone_value(&self, value: &dyn Any) -> Box<dyn Any + Send + Sync> {
        let value = value
            .downcast_ref::<T>()
            .expect("type mismatch cloning MutableState value");
        Box::new(value.clone())
    }

    fn merge_records(
        &self,
        previous: &dyn Any,
        current: &dyn Any,
        applied: &dyn Any,
    ) -> Option<Box<dyn Any + Send + Sync>> {
        let previous = previous
            .downcast_ref::<T>()
            .expect("type mismatch merging MutableState previous");
        let current = current
            .downcast_ref::<T>()
            .expect("type mismatch merging MutableState current");
        let applied = applied
            .downcast_ref::<T>()
            .expect("type mismatch merging MutableState applied");
        if let Some(merge) = self.merge.lock().unwrap().as_ref() {
            merge(previous, current, applied).map(|merged| Box::new(merged) as Box<dyn Any + Send + Sync>)
        } else {
            Some(Box::new(applied.clone()) as Box<dyn Any + Send + Sync>)
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::snapshots::{observe, with_mutable_snapshot, SnapshotApplyResult};
    use std::sync::Arc;

    #[test]
    fn global_state_updates_immediately() {
        let state = MutableState::new(0);
        assert_eq!(state.get(), 0);
        state.set(1);
        assert_eq!(state.get(), 1);
    }

    #[test]
    fn mutable_snapshot_isolation_and_apply() {
        let state = Arc::new(MutableState::new(10));
        let captured = state.clone();
        let result = with_mutable_snapshot(|| {
            assert_eq!(captured.get(), 10);
            captured.set(42);
            captured.get()
        })
        .expect("snapshot apply should succeed");
        assert_eq!(result, 42);
        assert_eq!(state.get(), 42);
    }

    #[test]
    fn read_only_snapshot_observes_changes() {
        let state = Arc::new(MutableState::new(5));
        use std::sync::atomic::{AtomicUsize, Ordering};

        let captured = state.clone();
        let reads = Arc::new(AtomicUsize::new(0));
        let reads_clone = reads.clone();
        let read_observer: Arc<dyn Fn(&dyn Any) + Send + Sync> =
            Arc::new(move |_any| {
                reads_clone.fetch_add(1, Ordering::Relaxed);
            });
        observe(
            Some(read_observer),
            None::<Arc<dyn Fn(&dyn Any) + Send + Sync>>,
            || {
                assert_eq!(captured.get(), 5);
            },
        );
        assert_eq!(reads.load(Ordering::Relaxed), 1);
    }

    #[test]
    fn conflicting_snapshots_fail_without_merge() {
        let state = Arc::new(MutableState::with_merge(0, |_, _, _| None));
        let result = with_mutable_snapshot(|| {
            state.set(2);
            let thread_state = state.clone();
            std::thread::spawn(move || {
                thread_state.set(1);
            })
            .join()
            .expect("thread should complete");
        });
        assert_eq!(result, Err(SnapshotApplyResult::Failure));
        assert_eq!(state.get(), 1);
    }
}
