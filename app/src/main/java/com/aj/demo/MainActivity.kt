package com.aj.demo

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.file_module.FileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//        val node = ThreadDemo.initP2P(7777, 7776)
//        Thread(Runnable{
//            Log.e("LogLogLog", "------> node:= $node     IP:= ${PublicIPFetcher.getPublicIP()}")
//            ThreadDemo.requestConnectToPeer("39.91.87.155", 7777)
//        }).start()

//        ThreadDemo.requestConnectToPeer("192.168.88.215", 105)
//        ThreadDemo.setP2PCallback { ip->
//            ThreadDemo.sendData(ip, data = "hello from main")
//        }
//        startActivity(Intent(this, RouterDemoActivity::class.java))

        val fileSystem = FileSystem()
        fileSystem.init(this)

//        for (i in 0..20) {
//            val save = fileSystem.saveUserProfile("40000$i", json)
//        }

        CoroutineScope(Dispatchers.IO).launch {
            delay(10000)
            withContext(Dispatchers.Main) {
//                val files = fileSystem.loadPrefetchDirectory("01", day = 0, flag = false)
                val files = fileSystem.deleteUserProfile("400000")
                Log.e("LogLogLog", "-----> files:= $files")
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
//        ThreadDemo.destroyP2P()
    }

    val json: String = """
        {"serviceType":1,"thirdPartyFeesAmount":0,"refundTipAmount":0,"discountAmount":0,"type":1,"serverId":"553901999792329216","orderCashDiscountAmount":0,"points":0,"number":960001,"posSn":"ANDVA7T5032232","numberPrefix":"P","itemDiscountAmount":0,"payAmount":94.03,"id":"189334236046187520","shopId":"485091432142801408","state":1,"thirdPartyOffsetAmount":0,"chargeItemList":[],"customProductList":[],"subTotalAmount":60,"pointAmount":0,"taxesFree":false,"refundTaxAmount":0,"orderVersion":2,"platformServiceFeeFree":false,"autoGratuityAmount":0,"removeAutoGratuity":false,"promotionAmount":0,"tableOrderInformation":{"tableName":"A6L","printNumber":false,"areaId":"820001080238145280","supportSeat":false,"areaName":"A1","guests":"3","tableId":"1011322171588869888","id":"1011322172251569920","shopId":"485091432142801408","diningTime":1},"code":"10039188","billingList":[],"refundSubTotalAmount":0,"serverName":"li ye","payState":0,"asap":true,"refundDeliveryFee":0,"posCreatorName":"li ye","convenienceFeePaymentType":0,"orderDishDetailsList":[{"additionAmount":0,"discountAmount":0,"menuName":"不在app卖的菜单","categoryName":"不在app卖的菜单分类11","seatNumber":1,"sentWhenVoid":false,"payAmount":50,"imageUrl":"","dishPrice":50,"menuId":"1096814861377601152","voided":false,"id":"189334269734837248","subTotalAmount":50,"voidedAddOnDetailsList":[],"aliasName":"app菜品2","orderAddOnDetailsList":[],"count":1,"apportionBaseAmount":50,"completed":true,"sendTime":"1749796103605","totalAmount":50,"promotionDescription":"","dishId":"1096815551495798400","dishName":"","send":true,"promotionAmount":0,"categoryId":"1096814961382391424"},{"specificationAliasName":"big","additionAmount":10,"discountAmount":0,"menuName":"不在app卖的菜单","categoryName":"不在app卖的菜单分类11","seatNumber":1,"sentWhenVoid":false,"payAmount":20,"imageUrl":"","dishPrice":10,"specificationPrice":10,"menuId":"1096814861377601152","voided":false,"id":"189334370129697795","specificationName":"大","subTotalAmount":20,"specificationId":"947847147775590016","voidedAddOnDetailsList":[],"aliasName":"app菜品4","orderAddOnDetailsList":[],"count":1,"apportionBaseAmount":20,"completed":true,"sendTime":"1749796112931","totalAmount":10,"promotionDescription":"","dishId":"1096815609029066368","dishName":"app菜品4","send":true,"promotionAmount":0,"categoryId":"1096814961382391424"}],"payLater":false,"platformServiceFeeAmount":6,"subPayAmount":60,"channelId":"481097491504499200","openTab":false,"orderTechInfo":{"handDevice":false,"orderId":"189334236046187520","isIosApp":false},"sumOfAllAmount":66,"itemCount":2,"lastPosSn":"ANDVA7T5032232","posCreatorId":"553901999792329216","createTime":"1749796099846","orderComboList":[],"refundDasherTip":0}
        """

}