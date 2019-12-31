package com.pushdy.views

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.pushdy.PDYConstant
import com.pushdy.Pushdy
import com.pushdy.R
import android.util.Log
import com.pushdy.core.entities.PDYParam
import com.pushdy.handlers.PDYDownloadImageHandler


open interface PDYPushBannerActionInterface {
    fun show(notification:Map<String, Any>, onTap:() -> Unit?)
}

open class PDYNotificationView : FrameLayout, View.OnClickListener, PDYPushBannerActionInterface {
    private var _notification:Map<String, Any>? = null

    private var _titleTV:TextView? = null
    private var _contentTV:TextView? = null
    private var _thumbIV:ImageView? = null
    private var _rootView:View? = null
    private var _onTap:(() -> Unit?)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val view = LayoutInflater.from(context).inflate(R.layout.view_in_app_banner, this, true)
        _rootView = view
        _titleTV = view.findViewById(R.id.tvTitle)
        _contentTV = view.findViewById(R.id.tvContent)
        _thumbIV = view.findViewById(R.id.ivThumb)

        _titleTV?.setOnClickListener(this)
        _contentTV?.setOnClickListener(this)
        _thumbIV?.setOnClickListener(this)
        val closeBtn:ImageView = view.findViewById(R.id.btnClose)
        closeBtn.setOnClickListener(OnClickListener { view ->
            hideView()
        })
    }

    companion object {
        private var _customMediaKey:String? = null

        @JvmStatic
        fun setCustomMediaKey(key:String) {
            _customMediaKey = key
        }

        @JvmStatic
        fun getCustomMediaKey() : String {
            return _customMediaKey ?: "media_url"
        }
    }

    override fun onClick(view: View?) {
        if (view == this || true) {
            _onTap?.invoke()
            hideView()
        }
    }

    fun mediaKey() : String {
        return _customMediaKey ?: "media_url"
    }

    private fun hideView() {
        val viewGroup = this.parent as? ViewGroup
        val view = this
        viewGroup?.post(Runnable {
            if (view != null) {
                viewGroup?.removeView(view)
            }
        })
    }

    override fun show(notification: Map<String, Any>, onTap: () -> Unit?) {
        _notification = notification
        _onTap = onTap
        //_rootView?.post(Runnable {
        //    if (_notification != null && _rootView != null) {
        //        if (_notification!!.containsKey("title")) {
        //            _titleTV?.text = _notification!!["title"] as String
        //        }
        //        if (_notification!!.containsKey("body")) {
        //            _contentTV?.text = _notification!!["body"] as String
        //        }
        //    }
        //})
        if (_notification!!.containsKey("title")) {
            _titleTV?.text = _notification!!["title"] as String
        }
        if (_notification!!.containsKey("body")) {
            _contentTV?.text = _notification!!["body"] as String
        }

        if (_thumbIV != null && _notification!!.containsKey("image")) {
            val mediaKey = _notification!!["image"] as? String
            var showImage = false
            if (mediaKey != null && mediaKey.startsWith("http")) {
                showImage = true
                PDYDownloadImageHandler(_thumbIV!!).execute(mediaKey)
            }

            _thumbIV?.post(Runnable {
                if (showImage) {
                    _thumbIV?.visibility = View.VISIBLE
                }
                else {
                    _thumbIV?.visibility = View.GONE
                }
            })
        }

        if (Pushdy.isPushBannerAutoDismiss()) {
            val delayHandler = Handler(Looper.getMainLooper())
            delayHandler.postDelayed(Runnable {
                hideView()
            }, (Pushdy.getPushBannerDismissDuration() * 1000.0).toLong())
        }
    }
}