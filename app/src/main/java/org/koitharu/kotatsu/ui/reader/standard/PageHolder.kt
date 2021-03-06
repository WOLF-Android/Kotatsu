package org.koitharu.kotatsu.ui.reader.standard

import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.android.synthetic.main.item_page.*
import kotlinx.coroutines.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import org.koitharu.kotatsu.ui.reader.PageLoader
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class PageHolder(parent: ViewGroup, private val loader: PageLoader) :
	BaseViewHolder<MangaPage, Unit>(parent, R.layout.item_page),
	SubsamplingScaleImageView.OnImageEventListener, CoroutineScope by loader {

	private var job: Job? = null

	init {
		ssiv.setOnImageEventListener(this)
		button_retry.setOnClickListener {
			doLoad(boundData ?: return@setOnClickListener, force = true)
		}
	}

	override fun onBind(data: MangaPage, extra: Unit) {
		doLoad(data, force = false)
	}

	override fun onRecycled() {
		job?.cancel()
		ssiv.recycle()
	}

	private fun doLoad(data: MangaPage, force: Boolean) {
		job?.cancel()
		job = launch {
			layout_error.isVisible = false
			progressBar.isVisible = true
			ssiv.recycle()
			try {
				val uri = withContext(Dispatchers.IO) {
					loader.loadFile(data.url, force)
				}.toUri()
				ssiv.setImage(ImageSource.uri(uri))
			} catch (e: CancellationException) {
				//do nothing
			} catch (e: Exception) {
				onError(e)
			}
		}
	}

	override fun onReady() {
		ssiv.maxScale = 2f * maxOf(ssiv.width / ssiv.sWidth.toFloat(), ssiv.height / ssiv.sHeight.toFloat())
		ssiv.resetScaleAndCenter()
	}

	override fun onImageLoadError(e: Exception) = onError(e)

	override fun onImageLoaded() {
		progressBar.isVisible = false
	}

	override fun onTileLoadError(e: Exception?) = Unit

	override fun onPreviewReleased() = Unit

	override fun onPreviewLoadError(e: Exception?) = Unit

	private fun onError(e: Throwable) {
		textView_error.text = e.getDisplayMessage(context.resources)
		layout_error.isVisible = true
		progressBar.isVisible = false
	}
}