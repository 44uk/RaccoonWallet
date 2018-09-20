package wacode.yamada.yuki.nempaymentapp.repository

import android.content.Context
import com.google.gson.Gson
import io.reactivex.Single
import wacode.yamada.yuki.nempaymentapp.NemPaymentApplication
import wacode.yamada.yuki.nempaymentapp.room.address.WalletInfo
import wacode.yamada.yuki.nempaymentapp.room.address.WalletInfoDao
import wacode.yamada.yuki.nempaymentapp.room.profile.MyProfile
import wacode.yamada.yuki.nempaymentapp.utils.SharedPreferenceUtils

class MyProfileRepository(val context: Context) {
    private val walletInfoDao: WalletInfoDao = NemPaymentApplication.database.walletInfoDao()

    fun createWalletInfo(entity: WalletInfo): Single<WalletInfo> {
        return Single.create { emitter ->
            entity.let {
                WalletInfo(walletInfoDao.create(it),
                        it.walletName,
                        it.walletAddress,
                        it.isMaster).let {
                    emitter.onSuccess(it)
                }
            }
        }
    }

    fun loadMyProfile(): Single<MyProfile> {
        return Single.create { emitter ->
            val myProfileString = SharedPreferenceUtils[context, KEY_PREF_MY_PROFILE, Gson().toJson(MyProfile())]
            emitter.onSuccess(Gson().fromJson(myProfileString, MyProfile::class.java))
        }
    }

    fun updateMyProfile(entity: MyProfile): Single<MyProfile> {
        return Single.create { emitter ->
            val myProfileString = Gson().toJson(entity)
            SharedPreferenceUtils.put(context, KEY_PREF_MY_PROFILE, myProfileString)
            emitter.onSuccess(entity)
        }
    }

    companion object {
        private const val KEY_PREF_MY_PROFILE = "key_pref_my_profile"
    }
}