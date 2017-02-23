package com.mishiranu.instantimage.ui

import android.app.{Activity, DownloadManager}
import android.content.{Context, Intent}
import android.content.pm.PackageManager
import android.net.Uri
import android.os.{Build, Bundle, Environment}
import android.view.ContextMenu.ContextMenuInfo
import android.view.{ContextMenu, Menu, MenuItem, View, ViewGroup, ViewTreeObserver}
import android.widget.{AdapterView, GridView, SearchView, Toast}

import com.mishiranu.instantimage.R
import com.mishiranu.instantimage.model.Image
import com.mishiranu.instantimage.util.{FileManager, Preferences}
import com.mishiranu.instantimage.util.ScalaHelpers._

import scala.collection.mutable

class FetchActivity extends Activity with ViewTreeObserver.OnGlobalLayoutListener with SearchView.OnQueryTextListener
  with MenuItem.OnActionExpandListener with LoadingDialog.Callback with AdapterView.OnItemClickListener {
  private val images = new mutable.ArrayBuffer[Image]
  private val gridAdapter = new GridAdapter()

  private var searchView: SearchView = _
  private var gridView: GridView = _

  private var keepSearchFocus: Boolean = false

  protected override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    getActionBar.setDisplayHomeAsUpEnabled(true)

    searchView = new SearchView(getActionBar.getThemedContext)
    searchView.setOnQueryTextListener(this)

    keepSearchFocus = savedInstanceState == null

    gridView = new GridView(this)
    gridView.setId(android.R.id.list)
    gridView.setOnItemClickListener(this)
    gridView.setAdapter(gridAdapter)
    val density = getResources.getDisplayMetrics.density
    val spacing = (FetchActivity.GRID_SPACING_DP * density + 0.5f).toInt
    gridView.setHorizontalSpacing(spacing)
    gridView.setVerticalSpacing(spacing)
    gridView.setPadding(spacing, spacing, spacing, spacing)
    gridView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY)
    gridView.setClipToPadding(false)
    gridView.getViewTreeObserver.addOnGlobalLayoutListener(this)
    registerForContextMenu(gridView)
    addContentView(gridView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT))

    val loadingDialog = getFragmentManager.findFragmentByTag(LoadingDialog.TAG).asInstanceOf[LoadingDialog]
    if (loadingDialog != null) {
      loadingDialog.setCallback(this)
    }
    if (savedInstanceState != null) {
      val images: java.util.ArrayList[Image] = savedInstanceState
        .getParcelableArrayList(FetchActivity.EXTRA_IMAGES)
      if (images != null) {
        import scala.collection.JavaConversions._
        this.images ++= images
        gridAdapter.setImages(this.images)
      }
    }
  }

  override protected def onSaveInstanceState(outState: Bundle): Unit = {
    super.onSaveInstanceState(outState)
    import scala.collection.JavaConverters._
    outState.putParcelableArrayList(FetchActivity.EXTRA_IMAGES, new java.util.ArrayList(images.asJava))
  }

  private var lastGridViewWidth = -1

  private val layoutRunnable: Runnable = () => {
    val position = gridView.getFirstVisiblePosition
    val density = getResources.getDisplayMetrics.density
    val sizeDp = if (getResources.getConfiguration.smallestScreenWidthDp >= 600) 200 else 150
    val size = (sizeDp * density + 0.5f).toInt
    val spacing = gridView.getHorizontalSpacing
    val numColumns = (lastGridViewWidth - spacing) / (size + spacing)
    gridView.setNumColumns(numColumns)
    gridView.setSelection(position)
  }

  override def onGlobalLayout(): Unit = {
    val width = gridView.getWidth
    if (lastGridViewWidth != width) {
      lastGridViewWidth = width
      gridView.removeCallbacks(layoutRunnable)
      gridView.post(layoutRunnable)
    }
  }

  override def onQueryTextChange(newText: String): Boolean = false

  override def onQueryTextSubmit(query: String): Boolean = {
    searchView.clearFocus()
    if (query.length() > 0) {
      new LoadingDialog(query, null, this).show(getFragmentManager, LoadingDialog.TAG)
    }
    false
  }

  override def onMenuItemActionExpand(item: MenuItem): Boolean = true

  override def onMenuItemActionCollapse(item: MenuItem): Boolean = {
    finish()
    false
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    menu.add(0, 1, 0, "").setActionView(searchView).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS
        | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW).setOnActionExpandListener(this).expandActionView()
    menu.add(0, 2, 0, R.string.text_crop_thumbnails).setCheckable(true)
        .setChecked(Preferences.cropThumbnails)
    if (keepSearchFocus) {
      keepSearchFocus = false
    } else {
      searchView.clearFocus()
    }
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case android.R.id.home =>
        finish()
        true
      case 2 =>
        Preferences.setCropThumbnails(!item.isChecked)
        gridAdapter.notifyDataSetChanged()
        invalidateOptionsMenu()
        true
      case _ =>
        super.onOptionsItemSelected(item)
    }
  }

  override def onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo): Unit = {
    menu.add(0, 1, 0, R.string.text_download)
  }

  override def onContextItemSelected(item: MenuItem): Boolean = {
    val info = item.getMenuInfo.asInstanceOf[AdapterView.AdapterContextMenuInfo]
    item.getItemId match {
      case 1 =>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
          checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
          requestPermissions(Array(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), info.position)
        } else {
          performDownload(gridAdapter.getItem(info.position))
        }
        true
      case _ =>
        super.onContextItemSelected(item)
    }
  }

  override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String],
    grantResults: Array[Int]): Unit = {
    if (grantResults.length == 1 && grantResults(0) == PackageManager.PERMISSION_GRANTED) {
      performDownload(gridAdapter.getItem(requestCode))
    }
  }

  private def performDownload(image: Image): Unit = {
    val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE).asInstanceOf[DownloadManager]
    val request = new DownloadManager.Request(Uri.parse(image.imageUriString))
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
        FileManager.obtainSimpleFileName(image.imageUriString))
    request.allowScanningByMediaScanner()
    try {
      downloadManager.enqueue(request)
    } catch {
      case e: IllegalArgumentException if e.getMessage.equals("Unknown URL content://downloads/my_downloads") =>
        Toast.makeText(this, R.string.error_download_manager, Toast.LENGTH_LONG).show()
    }
  }

  override def onQueryLoadingSuccess(images: List[Image]): Unit = {
    this.images.clear()
    this.images ++= images
    gridAdapter.setImages(images)
    gridView.post(() => gridView.setSelection(0))
  }

  override def onQueryLoadingError(errorMessageId: Int): Unit = {
    Toast.makeText(this, getString(errorMessageId), Toast.LENGTH_SHORT).show()
  }

  override def onImageLoadingSuccess(contentId: Int, mimeType: String): Unit = {
    val uri = ImageProvider.buildUri(contentId)
    setResult(Activity.RESULT_OK, new Intent().setDataAndType(uri, mimeType))
    finish()
  }

  override def onImageLoadingError(errorMessageId: Int): Unit = {
    onQueryLoadingError(errorMessageId)
  }

  override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long): Unit = {
    new LoadingDialog(null, gridAdapter.getItem(position).imageUriString, this)
      .show(getFragmentManager, LoadingDialog.TAG)
  }
}

object FetchActivity {
  private val EXTRA_IMAGES = "images"
  private val GRID_SPACING_DP = 4
}
