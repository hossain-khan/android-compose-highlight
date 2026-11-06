package android.webkit

class TestWebResourceError(
    private val code: Int,
    private val desc: CharSequence,
) : WebResourceError() {
    override fun getErrorCode(): Int = code

    override fun getDescription(): CharSequence = desc
}
