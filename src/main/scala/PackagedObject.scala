class PackagedObject(obj:ProxyObject,state:Boolean=false) {
  @volatile var ready=state
  @volatile var updated=false
  var proxy=obj
  var committed=false

  def write(newVal:Double) = {
    if(ready&&updated){
      if(proxy.innerObj.actualUse<proxy.localUsage){
        println(proxy.transaction.client+": ATTEMPT TO WRITE TO "+obj.innerObj.name+": "+newVal)
        proxy.innerObj.value=newVal
        proxy.innerObj.actualUse+=1
        if(proxy.localUsage==proxy.innerObj.actualUse){
          //Maximal number of uses has been reached.
          commit(proxy.innerObj.name)
        }
      }else{
        println("Object no longer available. Declared usage has been spent.")
      }
    }
    else println("Object is not ready to write yet.")
  }

  def read:Option[Double] = {
    if(ready&&updated){
      if(proxy.innerObj.actualUse<proxy.localUsage){
        proxy.innerObj.actualUse+=1
        if(proxy.innerObj.actualUse==proxy.localUsage) commit(proxy.innerObj.name)
        Some(proxy.innerObj.value)
      }else{
        println(proxy.transaction.client+"Object no longer available. Declared usage has been spent.")
        None
      }
    }
    else{
      println("Object is no longer available to read")
      None
    }
  }

  def commit(objName:String) = {
    if(!committed){
      proxy.transaction.self ! Commit(proxy.transaction,proxy)
      committed=true
    }
  }

}
