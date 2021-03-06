package wacode.yamada.yuki.nempaymentapp.viewmodel

import android.arch.lifecycle.MutableLiveData
import com.ryuta46.nemkotlin.model.Mosaic
import com.ryuta46.nemkotlin.model.MosaicDefinitionMetaDataPair
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import wacode.yamada.yuki.nempaymentapp.extentions.LoadingStatus
import wacode.yamada.yuki.nempaymentapp.extentions.LoadingStatusImpl
import wacode.yamada.yuki.nempaymentapp.rest.item.MosaicFullItem
import wacode.yamada.yuki.nempaymentapp.rest.item.MosaicItem
import wacode.yamada.yuki.nempaymentapp.rest.model.MosaicAppEntity
import wacode.yamada.yuki.nempaymentapp.usecase.EnterMosaicListUseCase
import javax.inject.Inject


class EnterMosaicListViewModel @Inject constructor(private val useCase: EnterMosaicListUseCase) : BaseViewModel(), LoadingStatus by LoadingStatusImpl() {
    val fullItemMosaic: MutableLiveData<MosaicFullItem> = MutableLiveData()

    fun getOwnedMosaicFullData(address: String) {
        useCase.getOwnedMosaics(address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .attachLoading()
                .subscribe({
                    getMosaicList(it)
                }, { it.printStackTrace() })
                .let { addDisposable(it) }
    }

    private fun getMosaicList(list: List<Mosaic>) {
        val nameSpaceHashMap = HashMap<String, List<Mosaic>>()
        containsXemNemSendObserve(list)
        for (namespaceMosaic in list) {
            val namespaceList = ArrayList<Mosaic>()
            list.filterTo(namespaceList) { namespaceMosaic.mosaicId.namespaceId == it.mosaicId.namespaceId }
            nameSpaceHashMap.put(namespaceMosaic.mosaicId.namespaceId, namespaceList)
        }
        for (key in nameSpaceHashMap.keys) {
            useCase.getNamespaceMosaics(key)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .attachLoading()
                    .subscribe({ getFullMosaicItems(nameSpaceHashMap, key, it) },
                            { it.printStackTrace() })
                    .let { addDisposable(it) }
        }
    }

    private fun containsXemNemSendObserve(list: List<Mosaic>) {
        list
                .map { MosaicItem(MosaicAppEntity.convert(it)) }
                .filter { it.isNEMXEMItem() }
                .forEach { fullItemMosaic.value = MosaicFullItem(0, it) }
    }

    private fun getFullMosaicItems(nameSpaceHashMap: HashMap<String, List<Mosaic>>, key: String, responseList: List<MosaicDefinitionMetaDataPair>) {
        val mosaicList = nameSpaceHashMap[key]
        mosaicList?.let {
            for (responseItem in responseList) {
                it
                        .filter { responseItem.mosaic.id.fullName == it.mosaicId.fullName }
                        .forEach { item ->
                            responseItem.mosaic.divisibility?.let {
                                fullItemMosaic.value = MosaicFullItem(it, MosaicItem(MosaicAppEntity.convert(item)))
                            }
                        }
            }
        }
    }
}
