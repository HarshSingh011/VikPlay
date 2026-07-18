package com.example.vidplay.Navigation

object Routes {
    const val LOGIN              = "login"
    const val REGISTER           = "register"
    const val OTP                = "otp/{email}"
    const val OTP_FORGOT_PASSWORD = "otpForgotPassword/{email}"
    const val EMAIL_VERIFY       = "emailVerify"
    const val FORGOT_PASSWORD    = "forgotPassword/{email}"
    
    const val MAIN         = "main"
    const val PAGE1        = "page1"
    const val VIDEO_PLAYER = "videoPlayer/{videoUri}"
    const val TOKEN_PAGE   = "tokenPage"
    const val STREAMING    = "streaming"
    const val LIVE_STREAM  = "liveStream"
    
    const val VIEW_STREAM  = "viewStream/{streamCode}?streamTitle={streamTitle}"
    const val CALL          = "call"
    const val LOCAL_STORAGE = "localStorage"
}
