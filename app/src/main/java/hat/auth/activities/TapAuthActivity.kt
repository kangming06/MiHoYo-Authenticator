package hat.auth.activities

import android.os.Bundle
import hat.auth.activities.tap.*
import hat.auth.utils.ui.ComposeActivity

class TapAuthActivity : ComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init {
            UI()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyWebView()
    }

}
