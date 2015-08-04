/**
 * Created by prophet on 07.02.15.
 */
class ProxyObject(trans:Transaction, obj:SharedObject, sup:Int) {
  var innerObj = obj
  val supremum = sup
  val transaction=trans
  var localUsage = innerObj.declaredUse
//  println("PROXY CREATED. LOCALUSAGE = "+innerObj.declaredUse)
}
