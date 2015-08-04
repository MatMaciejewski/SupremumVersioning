class ProxyObject(trans:Transaction, obj:SharedObject, sup:Int) {
  var innerObj = obj
  val supremum = sup
  val transaction=trans
  var localUsage = innerObj.declaredUse
}
