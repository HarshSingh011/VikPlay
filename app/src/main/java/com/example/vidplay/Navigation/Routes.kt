package com.example.vidplay.Navigation

object Routes {
    // Auth Routes
    const val LOGIN              = "login"
    const val REGISTER           = "register"
    const val OTP                = "otp/{email}"
    const val EMAIL_VERIFY       = "emailVerify"
    const val FORGOT_PASSWORD    = "forgotPassword/{email}"
    
    const val MAIN         = "main"
    const val PAGE1        = "page1"
    const val VIDEO_PLAYER = "videoPlayer/{videoUri}"
    const val TOKEN_PAGE   = "tokenPage"
    const val STREAMING    = "streaming"
    const val LIVE_STREAM  = "liveStream"
    /** Viewer screen — args: streamCode (path), streamTitle (query, URL-encoded) */
    const val VIEW_STREAM  = "viewStream/{streamCode}?streamTitle={streamTitle}"
    const val CALL          = "call"
    const val LOCAL_STORAGE = "localStorage"
}
