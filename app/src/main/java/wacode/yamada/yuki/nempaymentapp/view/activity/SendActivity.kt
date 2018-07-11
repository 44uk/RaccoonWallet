package wacode.yamada.yuki.nempaymentapp.view.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg
import wacode.yamada.yuki.nempaymentapp.R
import wacode.yamada.yuki.nempaymentapp.extentions.convertNEMFromMicroToDouble
import wacode.yamada.yuki.nempaymentapp.helper.PinCodeHelper
import wacode.yamada.yuki.nempaymentapp.model.PaymentQREntity
import wacode.yamada.yuki.nempaymentapp.rest.item.SendMosaicItem
import wacode.yamada.yuki.nempaymentapp.utils.WalletManager
import wacode.yamada.yuki.nempaymentapp.view.dialog.RaccoonAlertType
import wacode.yamada.yuki.nempaymentapp.view.dialog.RaccoonAlertViewModel
import wacode.yamada.yuki.nempaymentapp.view.dialog.RaccoonErrorDialog
import wacode.yamada.yuki.nempaymentapp.view.fragment.BaseFragment
import wacode.yamada.yuki.nempaymentapp.view.fragment.send.*

class SendActivity : BaseFragmentActivity() {
    override fun initialFragment() = replaceFragmentForLaunch()

    override fun setLayout() = SIMPLE_FRAGMENT_ONLY_LAYOUT

    private val address by lazy {
        intent.getStringExtra(KEY_SEND_ADDRESS)
    }

    private val screenType by lazy {
        return@lazy if (intent.hasExtra(KEY_SEND_SCREEN_TYPE)) {
            intent.getSerializableExtra(KEY_SEND_SCREEN_TYPE) as SendType
        } else {
            SendType.ENTER
        }
    }

    private val senderPublicKey by lazy {
        return@lazy if (intent.getStringExtra(KEY_SENDER_PUBLIC_KEY).isNullOrEmpty()) {
            ""
        } else {
            intent.getStringExtra(KEY_SENDER_PUBLIC_KEY)
        }
    }

    private val paymentEntity by lazy {
        intent.getSerializableExtra(KEY_SEND_PAYMENT_ENTITY) as PaymentQREntity
    }

    private val intentSendArray by lazy {
        val list = ArrayList<SendMosaicItem>()
        list.add(SendMosaicItem.createNEMXEMItem(paymentEntity.data.amount.convertNEMFromMicroToDouble()))
        return@lazy list
    }

    private val viewModel = SendViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showErrorDialogIfNeeded()

        viewModel.replaceEvent
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ item ->
                    replaceFragment(item.first, item.second)
                })
        setToolBarBackButton()

    }

    private fun showErrorDialogIfNeeded() {
        async {
            val wallet = bg { WalletManager.getSelectedWallet(this@SendActivity) }.await()
            when {
                wallet == null -> showWalletErrorDialog()
                !PinCodeHelper.isAvailable(this@SendActivity) -> showPinCodeErrorDialog()
            }
        }
    }

    private fun showPinCodeErrorDialog() {
        val viewModel = RaccoonAlertViewModel()
        viewModel.clickEvent
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        RaccoonAlertType.BOTTOM_BUTTON -> {
                            startActivity(SettingActivity.getCallingIntent(this))
                            finish()
                        }
                        RaccoonAlertType.CLOSE_BUTTON -> finish()
                    }
                }

        viewModel.closeEvent
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { finish() }

        val dialog = RaccoonErrorDialog.createDialog(viewModel,
                getString(R.string.raccoon_error_pin_title),
                getString(R.string.raccoon_error_pin_message),
                getString(R.string.raccoon_error_pin_button))
        dialog.show(this.supportFragmentManager, "")
    }

    private fun showWalletErrorDialog() {
        val viewModel = RaccoonAlertViewModel()
        viewModel.clickEvent
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        RaccoonAlertType.BOTTOM_BUTTON -> {
                            startActivity(SelectWalletActivity.createIntent(this))
                            finish()
                        }
                        RaccoonAlertType.CLOSE_BUTTON -> finish()
                    }
                }

        viewModel.closeEvent
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { finish() }

        val dialog = RaccoonErrorDialog.createDialog(viewModel,
                getString(R.string.raccoon_error_pin_title),
                getString(R.string.raccoon_error_wallet_message),
                getString(R.string.raccoon_error_wallet_button))
        dialog.show(this.supportFragmentManager, "")
    }

    private fun replaceFragmentForLaunch(): BaseFragment {
        return when (screenType) {
            SendType.SELECT_MODE -> SelectSendModeFragment.newInstance(viewModel, intentSendArray)
            SendType.SELECT_MESSAGE -> SelectMessageTypeFragment.newInstance(viewModel, intentSendArray, senderPublicKey != "")
            SendType.CONFIRM -> SendConfirmFragment.newInstance(viewModel, intentSendArray, address, paymentEntity.data.msg, senderPublicKey)
            else -> EnterSendFragment.newInstance(viewModel)
        }
    }

    private fun replaceFragment(sendType: SendType, list: ArrayList<SendMosaicItem>) {
        when (sendType) {
            SendType.ENTER -> replaceEnter()
            SendType.SELECT_MODE -> replaceSelectMode(list)
            SendType.CONFIRM -> replaceConfirm(list)
            SendType.SELECT_MESSAGE -> replaceSelectMessageType(list)
            SendType.MESSAGE -> replaceMessage(list)
            SendType.FINISH -> replaceFinish()
        }
    }

    private fun replaceEnter() {
        replaceFragment(EnterSendFragment.newInstance(viewModel), true)
    }

    private fun replaceSelectMode(list: ArrayList<SendMosaicItem>) {
        replaceFragment(SelectSendModeFragment.newInstance(viewModel, list), true)
    }

    private fun replaceConfirm(list: ArrayList<SendMosaicItem>, message: String = "") {
        replaceFragment(SendConfirmFragment.newInstance(viewModel, list, address, message, senderPublicKey), true)
    }

    private fun replaceSelectMessageType(list: ArrayList<SendMosaicItem>) {
        replaceFragment(SelectMessageTypeFragment.newInstance(viewModel, list, senderPublicKey != ""), true)
    }

    private fun replaceMessage(list: ArrayList<SendMosaicItem>) {
        val intent = SendMessageActivity.createIntent(this)
        intent.putExtra(KEY_SEND_MESSAGE_LIST, list)
        startActivityForResult(intent, REQUEST_CODE_MESSAGE)
    }

    private fun replaceFinish() {
        replaceFragment(SendFinishFragment.newInstance(), true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_MESSAGE) {
            data?.let {
                replaceConfirm(it.getSerializableExtra(KEY_SEND_MESSAGE_LIST) as ArrayList<SendMosaicItem>, message = it.getStringExtra(SendMessageActivity.INTENT_MESSAGE))
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_MESSAGE = 1129
        const val KEY_SEND_MESSAGE_LIST = "key_send_message_list"
        private const val KEY_SEND_ADDRESS = "key_send_address"
        private const val KEY_SENDER_PUBLIC_KEY = "key_sender_public_key"
        private const val KEY_SEND_PAYMENT_ENTITY = "key_send_payment_entity"
        private const val KEY_SEND_SCREEN_TYPE = "key_send_screen_type"

        fun createIntent(context: Context, address: String, senderPublicKey: String): Intent {
            val intent = Intent(context, SendActivity::class.java)
            intent.putExtra(KEY_SEND_ADDRESS, address)
            intent.putExtra(KEY_SENDER_PUBLIC_KEY, senderPublicKey)
            return intent
        }

        fun createIntent(context: Context, address: String, senderPublicKey: String?, screenType: SendType, paymentQREntity: PaymentQREntity?): Intent {
            val intent = Intent(context, SendActivity::class.java)
            intent.putExtra(KEY_SEND_ADDRESS, address)
            intent.putExtra(KEY_SENDER_PUBLIC_KEY, senderPublicKey)
            intent.putExtra(KEY_SEND_PAYMENT_ENTITY, paymentQREntity)
            intent.putExtra(KEY_SEND_SCREEN_TYPE, screenType)
            return intent
        }
    }
}

class SendViewModel {
    var sendMessageType: SendMessageType = SendMessageType.NONE
    val replaceEvent: PublishSubject<Pair<SendType, ArrayList<SendMosaicItem>>> = PublishSubject.create()

    fun replaceFragment(sendType: SendType, list: ArrayList<SendMosaicItem>) {
        replaceEvent.onNext(Pair(sendType, list))
    }

    fun putMessageTypeNormal() {
        sendMessageType = SendMessageType.NORMAL
    }

    fun putMessageTypeCrypt() {
        sendMessageType = SendMessageType.CRYPT
    }
}

enum class SendType {
    ENTER,
    SELECT_MODE,
    MESSAGE,
    SELECT_MESSAGE,
    CONFIRM,
    FINISH
}

enum class SendMessageType {
    NONE,
    NORMAL,
    CRYPT
}
