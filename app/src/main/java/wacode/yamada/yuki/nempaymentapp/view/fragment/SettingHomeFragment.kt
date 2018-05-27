package wacode.yamada.yuki.nempaymentapp.view.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_setting_home.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg
import wacode.yamada.yuki.nempaymentapp.R
import wacode.yamada.yuki.nempaymentapp.extentions.showToast
import wacode.yamada.yuki.nempaymentapp.helper.FingerprintHelper
import wacode.yamada.yuki.nempaymentapp.helper.PinCodeHelper
import wacode.yamada.yuki.nempaymentapp.preference.AppLockPreference
import wacode.yamada.yuki.nempaymentapp.preference.FingerprintPreference
import wacode.yamada.yuki.nempaymentapp.preference.PinCodePreference
import wacode.yamada.yuki.nempaymentapp.utils.PinCodeProvider
import wacode.yamada.yuki.nempaymentapp.utils.WalletManager
import wacode.yamada.yuki.nempaymentapp.utils.WalletProvider
import wacode.yamada.yuki.nempaymentapp.view.activity.NewCheckPinCodeActivity
import wacode.yamada.yuki.nempaymentapp.view.activity.NewPinCodeSettingActivity
import wacode.yamada.yuki.nempaymentapp.view.activity.PrivateKeyStoreSupportActivity
import wacode.yamada.yuki.nempaymentapp.view.custom_view.SettingSwitchItemView
import wacode.yamada.yuki.nempaymentapp.view.dialog.*


class SettingHomeFragment : BaseFragment() {
    override fun layoutRes() = R.layout.fragment_setting_home

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    private fun setupView() {
        selectNodeButton.setOnClickListener { replaceFragment(SelectNodeFragment.newInstance(), true) }
        languageSettingButton.setOnClickListener { context.showToast(R.string.com_coming_soon) }
        notificationSettingButton.setOnClickListener { context.showToast(R.string.com_coming_soon) }
        passwordSettingButton.setOnClickListener { showSettingPinCode() }

        setupFingerprintSwitch()
        setupAppLockSwitch()
        setupSecurityLessonClick()
    }

    override fun onResume() {
        super.onResume()
        if (!FingerprintManagerCompat.from(context).hasEnrolledFingerprints()) {
            FingerprintPreference.saveFingerprintSetting(context, false)
        }

        val isValidFingerprint = FingerprintPreference.getFingerprintSetting(context)
        fingerprintSetting.setCheck(isValidFingerprint)
    }

    private fun setupFingerprintSwitch() {
        fingerprintSetting.setOnSwitchClickListener(object : SettingSwitchItemView.OnSwitchClickListener {
            override fun onClick(isCheck: Boolean) {
                if (isCheck) {
                    fingerprintSetting.setCheck(false)
                    if (FingerprintHelper.checkForSave(context)) {
                        showPinCodeChecking(REQUEST_CODE_FINGERPRINT_SETTING)
                    } else {
                        showNotAvailableFingerprintDialog()
                    }
                } else {
                    PinCodePreference.removePinCodeForFingerprint(context)
                    FingerprintPreference.saveFingerprintSetting(context, false)
                }
            }
        })
    }

    private fun setupAppLockSwitch() {
        val isAvailableAppLock = AppLockPreference.isAvailable(context)
        appLockSetting.setCheck(isAvailableAppLock)

        appLockSetting.setOnSwitchClickListener(object : SettingSwitchItemView.OnSwitchClickListener {
            override fun onClick(isCheck: Boolean) {
                if (isCheck) {
                    if (PinCodeHelper.isAvailable(context)) {
                        AppLockPreference.save(context, true)
                        context.showToast(R.string.setting_app_lock_complete_message)
                    } else {
                        appLockSetting.setCheck(false)
                        showNotAvailablePinCodeDialog()
                    }
                } else {
                    AppLockPreference.save(context, false)
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                NewCheckPinCodeActivity.REQUEST_CODE_PIN_CODE_CHECK -> {
                    val code = data?.getByteArrayExtra(NewPinCodeSettingActivity.INTENT_PIN_CODE)
                    code?.let {
                        PinCodeProvider.setPinCode(it.toString(Charsets.UTF_8))
                        startActivityForResult(
                                NewPinCodeSettingActivity.getCallingIntent(context, NewPinCodeSettingActivity.NextAction.BACK_FOR_RESULT),
                                NewPinCodeSettingActivity.REQUEST_CODE_PIN_CODE_SETTING)
                    }

                }
                NewPinCodeSettingActivity.REQUEST_CODE_PIN_CODE_SETTING -> {
                    PinCodeProvider.clearCache()
                    context.showToast(R.string.setting_pin_code_success)

                }
                REQUEST_CODE_FINGERPRINT_SETTING -> {
                    val code = data?.getByteArrayExtra(NewPinCodeSettingActivity.INTENT_PIN_CODE)
                    code?.let {
                        showFingerprintSettingDialog(it)
                    }
                }
                REQUEST_CODE_TUTORIAL_CODE -> {
                    val code = data?.getByteArrayExtra(NewPinCodeSettingActivity.INTENT_PIN_CODE)
                    code?.let {
                        PinCodeProvider.setPinCode(it.toString(Charsets.UTF_8))
                        startActivity(PrivateKeyStoreSupportActivity.createIntent(context))
                    }
                }
            }
        }
    }

    private fun showPinCodeChecking(requestCode: Int) {
        if (PinCodeHelper.isAvailable(context)) {
            startActivityForResult(NewCheckPinCodeActivity.getCallingIntent(context = context,
                    isDisplayFingerprint = false,
                    messageRes = R.string.setting_pin_code_check_title,
                    buttonPosition = NewCheckPinCodeActivity.ButtonPosition.RIGHT
            ), requestCode)
        } else {
            showNotAvailablePinCodeDialog()
        }
    }

    private fun showSettingPinCode() {
        if (PinCodeHelper.isAvailable(context)) {
            startActivityForResult(NewCheckPinCodeActivity.getCallingIntent(context = context,
                    isDisplayFingerprint = false,
                    messageRes = R.string.setting_pin_code_check_title,
                    buttonPosition = NewCheckPinCodeActivity.ButtonPosition.LEFT),
                    NewCheckPinCodeActivity.REQUEST_CODE_PIN_CODE_CHECK)
        } else {
            startActivityForResult(
                    NewPinCodeSettingActivity.getCallingIntent(context, NewPinCodeSettingActivity.NextAction.BACK),
                    NewPinCodeSettingActivity.REQUEST_CODE_PIN_CODE_SETTING)
        }
    }

    private fun showNotAvailablePinCodeDialog() {
        val viewModel = RaccoonConfirmViewModel()
        RaccoonConfirmDialog.createDialog(viewModel,
                getString(R.string.setting_pincodd_not_available_title),
                getString(R.string.setting_pincodd_not_available_message),
                getString(R.string.com_ok))
                .show(activity.supportFragmentManager, "")
    }

    private fun setupSecurityLessonClick() {
        securityLessonLayout.setOnClickListener {
            showProgress()
            async(UI) {
                val wallet = bg { WalletManager.getSelectedWallet(this@SettingHomeFragment.context) }.await()
                WalletProvider.wallet = wallet

                hideProgress()

                if (PinCodeHelper.isAvailable(this@SettingHomeFragment.context)) {
                    startActivityForResult(NewCheckPinCodeActivity.getCallingIntent(
                            context = this@SettingHomeFragment.context,
                            isDisplayFingerprint = true,
                            buttonPosition = NewCheckPinCodeActivity.ButtonPosition.RIGHT
                    ), REQUEST_CODE_TUTORIAL_CODE)
                } else {
                    startActivity(PrivateKeyStoreSupportActivity.createIntent(this@SettingHomeFragment.context))
                }
            }
        }
    }

    private fun showNotAvailableFingerprintDialog() {
        val viewModel = RaccoonSelectViewModel(getString(R.string.com_ok), getString(R.string.com_cancel))

        viewModel.clickEvent
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { item ->
                    if (item == SelectDialogButton.POSITIVE) {
                        startActivity(Intent().setAction(Settings.ACTION_SECURITY_SETTINGS))
                    }
                }

        RaccoonSelectDialog.createDialog(viewModel,
                getString(R.string.pin_code_setting_end_not_available_title),
                getString(R.string.pin_code_setting_end_not_available_message))
                .show(activity.supportFragmentManager, "")
    }

    private fun showFingerprintSettingDialog(data: ByteArray) {
        val viewModel = FingerprintSettingViewModel()

        viewModel.clickEvent
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { item ->
                    when (item) {
                        FingerprintSettingSelectButton.POSITIVE -> {
                            fingerprintSetting.setCheck(true)
                            PinCodePreference.savePinCodeForFingerprint(context, data.toString(Charsets.UTF_8))
                        }
                        FingerprintSettingSelectButton.COMPLETE -> {
                            context.showToast(R.string.setting_fingerprint_success)
                        }
                    }
                }

        FingerprintSettingDialog.createDialog(viewModel).show(activity.supportFragmentManager, "")
    }

    companion object {
        const val REQUEST_CODE_FINGERPRINT_SETTING = 1113
        const val REQUEST_CODE_TUTORIAL_CODE = 1114

        fun newInstance(): SettingHomeFragment {
            val fragment = SettingHomeFragment()
            val args = Bundle().apply {
                putInt(ARG_CONTENTS_NAME_ID, R.string.setting_home_title)
            }
            fragment.arguments = args
            return fragment
        }
    }
}