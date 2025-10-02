package com.archstarter.feature.catalog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.MocksMap
import com.archstarter.core.common.presenter.PresenterMockKey
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.feature.catalog.api.CatalogItem
import com.archstarter.feature.catalog.api.CatalogItemPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Suppress("unused")
private val ensureCatalogItemMocks = CatalogItemPresenterMocks

@Composable
fun CatalogItemCard(
  id: Int,
  modifier: Modifier = Modifier,
) {
  val presenter = rememberPresenter<CatalogItemPresenter, Int>(key = "item$id", params = id)
  val state by presenter.state.collectAsStateWithLifecycle()
  CatalogItemCardContent(
    state = state,
    onClick = presenter::onClick,
    modifier = modifier
  )
}

@Composable
internal fun CatalogItemCardContent(
  state: CatalogItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(12.dp)
  ) {
    Text(state.title)
    Text(state.summary, style = MaterialTheme.typography.bodySmall)
  }
}

@Preview
@Composable
private fun PreviewCatalogItemCard() {
  AppTheme { CatalogItemCard(id = 1) }
}

private object CatalogItemPresenterMocks {
  private val presenters = mapOf(
    "item1" to FakeCatalogItemPresenter(1),
    "item2" to FakeCatalogItemPresenter(2),
    "item3" to FakeCatalogItemPresenter(3),
  )

  init {
    if (BuildConfig.DEBUG) {
      presenters.forEach { (key, presenter) ->
        MocksMap[PresenterMockKey(CatalogItemPresenter::class, key)] = presenter
      }
    }
  }
}

private class FakeCatalogItemPresenter(
  private val defaultId: Int,
) : CatalogItemPresenter {
  private val _state = MutableStateFlow(createState(defaultId))
  override val state: StateFlow<CatalogItem> = _state

  override fun initOnce(params: Int?) {
    val id = params ?: defaultId
    _state.value = createState(id)
  }

  override fun onClick() {}

  private fun createState(id: Int) = CatalogItem(
    id = id,
    title = "Item #$id",
    summary = "Summary for item #$id",
  )
}
