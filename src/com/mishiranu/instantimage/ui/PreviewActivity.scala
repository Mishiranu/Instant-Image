package com.mishiranu.instantimage.ui

import android.app.{Activity, DownloadManager}
import android.content.{Context, Intent}
import android.content.pm.PackageManager
import android.net.Uri
import android.os.{Build, Bundle, Environment, Parcelable}
import android.text.{SpannableStringBuilder, Spanned}
import android.text.style.TypefaceSpan
import android.view.{Gravity, Menu, MenuItem, View, ViewGroup}
import android.widget.{Button, ImageView, LinearLayout, TextView, Toast}

import com.mishiranu.instantimage.R
import com.mishiranu.instantimage.model.Image
import com.mishiranu.instantimage.util.FileManager
import com.mishiranu.instantimage.util.ScalaHelpers._
import com.squareup.picasso.Picasso

class PreviewActivity extends Activity {
  import PreviewActivity._

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    getActionBar.setDisplayHomeAsUpEnabled(true)

    sdk(21) { case true =>
      getWindow.setSharedElementEnterTransition(getWindow.getSharedElementEnterTransition.clone.setDuration(150))
      getWindow.setSharedElementReturnTransition(getWindow.getSharedElementReturnTransition.clone.setDuration(150))
    }

    val image = getIntent.getParcelableExtra[Image](EXTRA_IMAGE)
    val transition = getIntent.getStringExtra(EXTRA_TRANSITION)

    val density = getResources.getDisplayMetrics.density
    val padding = (16 * density + 0.5f).toInt

    val rootLayout = new LinearLayout(this)
    rootLayout.setOrientation(LinearLayout.VERTICAL)
    addContentView(rootLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT))
    rootLayout.setOnClickListener((_: View) => finishWithTransition())

    val imageView = new ImageView(this)
    rootLayout.addView(imageView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1))
    Picasso.`with`(this).load(image.thumbnailUriString).into(imageView)
    sdk(21) { case true =>
      imageView.setTransitionName(transition)
    }

    val innerLayout = new LinearLayout(this)
    innerLayout.setOrientation(LinearLayout.VERTICAL)
    innerLayout.setPadding(padding, padding, padding, padding)
    rootLayout.addView(innerLayout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    val urlView = new TextView(this, null, android.R.attr.textAppearanceListItem)
    val thumbnailUrl = new SpannableStringBuilder("URL: ").append(image.imageUriString)
    thumbnailUrl.setSpan(new TypefaceSpan("sans-serif-medium"), 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    urlView.setText(thumbnailUrl)
    innerLayout.addView(urlView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    val button = new Button(this, null, android.R.attr.borderlessButtonStyle)
    val selectedCount: Integer = getIntent.getIntExtra(EXTRA_SELECTED_COUNT, 1)
    button.setText(getResources.getQuantityString(R.plurals.text_select_image_format, selectedCount, selectedCount))
    val buttonLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT)
    buttonLayoutParams.gravity = Gravity.RIGHT
    innerLayout.addView(button, buttonLayoutParams)
    button.setOnClickListener { (_: View) =>
      setResult(Activity.RESULT_OK, new Intent().putExtra(EXTRA_IMAGE, image.asInstanceOf[Parcelable]))
      finishWithTransition()
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    menu.add(0, OPTIONS_ITEM_DOWNLOAD, 0, R.string.text_download).setIcon(sdk(21) {
      case true => R.drawable.ic_file_download_white
      case false => R.drawable.ic_action_download
    }.get).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case android.R.id.home =>
        finishWithTransition()
        true
      case OPTIONS_ITEM_DOWNLOAD =>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
          checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
          requestPermissions(Array(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSION)
        } else {
          performDownload()
        }
        true
      case _ =>
        super.onOptionsItemSelected(item)
    }
  }

  override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String],
    grantResults: Array[Int]): Unit = {
    if (requestCode == REQUEST_CODE_PERMISSION &&
      grantResults.length == 1 && grantResults(0) == PackageManager.PERMISSION_GRANTED) {
      performDownload()
    }
  }

  private def performDownload(): Unit = {
    val image = getIntent.getParcelableExtra[Image](EXTRA_IMAGE)
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

  private def finishWithTransition(): Unit = {
    sdk(21) {
      case true => finishAfterTransition()
      case false => finish()
    }
  }
}

object PreviewActivity {
  val EXTRA_IMAGE: String = "image"
  val EXTRA_SELECTED_COUNT: String = "selected_count"
  val EXTRA_TRANSITION: String = "transition"

  private val REQUEST_CODE_PERMISSION = 1

  private val OPTIONS_ITEM_DOWNLOAD = 1
}
