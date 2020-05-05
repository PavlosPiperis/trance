package shredding.examples.tpch

import shredding.core._
import shredding.examples.Query
import shredding.nrc.MaterializeNRC


/** Benchmark Queries: Nested to Nested **/

object Test0NN extends TPCHBase {

  val name = "Test0NN"
  override def indexedDict: List[String] = List(s"${name}__D_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("L", "P").contains(x._1)).values.toList.mkString("")}"
 
  val partsInput = Test0Full.program(Test0Full.name).varRef.asInstanceOf[BagExpr]
  val (parts, partRef) = varset(Test0Full.name, "l", partsInput)
  val query = 
    ReduceByKey(ForeachUnion(partRef, parts,
      ForeachUnion(pr, relP,
        IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
          Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
    List("p_name"), List("l_quantity"))

  val program = Program(Assignment(name, query))

}

object Test0Push extends TPCHBase {

  val name = "Test0Push"
  override def indexedDict: List[String] = List(s"${name}__D_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("L", "P").contains(x._1)).values.toList.mkString("")}"
 
  val partsInput = Test0Full.program(Test0Full.name).varRef.asInstanceOf[BagExpr]
  val (parts, partRef) = varset(Test0Full.name, "l", partsInput)
  val pushAgg = ReduceByKey(parts, List("l_partkey"), List("l_quantity"))
  val (partsAgg, partsAggRef) = varset("localAgg", "l2", pushAgg)
  val query = 
    ReduceByKey(ForeachUnion(partsAggRef, partsAgg,
      ForeachUnion(pr, relP,
        IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
          Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
    List("p_name"), List("l_quantity"))

  val program = Program(Assignment(partsAgg.name, pushAgg), Assignment(name, query))

}

object Test0FullNN extends TPCHBase {

  val name = "Test0FullNN"
  override def indexedDict: List[String] = List(s"${name}__D_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("L", "P").contains(x._1)).values.toList.mkString("")}"
 
  val partsInput = Test0Full.program(Test0Full.name).varRef.asInstanceOf[BagExpr]
  val (parts, partRef) = varset(Test0Full.name, "l", partsInput)
  val query = 
    ReduceByKey(ForeachUnion(partRef, parts,
      ForeachUnion(pr, relP,
        IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
          Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))), 
    List("p_name"), List("l_quantity"))

  val program = Program(Assignment(name, query))

}

object Test1NN extends TPCHBase {

  val name = "Test1NN"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("O", "L", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (orders, orderRef) = varset(Test1Full.name, "o", Test1Full.program(Test1Full.name).varRef.asInstanceOf[BagExpr])
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val query = 
    ForeachUnion(orderRef, orders,
      Singleton(Tuple("o_orderdate" -> orderRef("o_orderdate"), "o_parts" ->
        ReduceByKey(ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
          ForeachUnion(pr, relP,
            IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
              Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
            List("p_name"), List("l_quantity")))))

  val program = Program(Assignment(name, query))

}

object Test1FullNN extends TPCHBase {

  val name = "Test1FullNN"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("O", "L", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (orders, orderRef) = varset(Test1Full.name, "o", Test1Full.program(Test1Full.name).varRef.asInstanceOf[BagExpr])
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val query = 
    ForeachUnion(orderRef, orders,
      projectTuple(orderRef, "o_parts" ->
        ReduceByKey(ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
          ForeachUnion(pr, relP,
            IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
              Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
            List("p_name"), List("l_quantity"))))

  val program = Program(Assignment(name, query))

}

object Test2NN extends TPCHBase {

  val name = "Test2NN"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"c__Dc_orders_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("C", "O", "L", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (customers, customerRef) = varset(Test2Full.name, "c", Test2Full.program(Test2Full.name).varRef.asInstanceOf[BagExpr])
  val (orders, orderRef) = varset("orders", "o", BagProject(customerRef, "c_orders"))
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val query = 
  ForeachUnion(customerRef, customers,
    Singleton(Tuple("c_name" -> customerRef("c_name"), "c_orders" -> 
      ForeachUnion(orderRef, BagProject(customerRef, "c_orders"),
        Singleton(Tuple("o_orderdate" -> orderRef("o_orderdate"), "o_parts" ->
          ReduceByKey(ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
            ForeachUnion(pr, relP,
              IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
                Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
          List("p_name"), List("l_quantity"))))))))

  val program = Program(Assignment(name, query))
}

object Test2FullNN extends TPCHBase {

  val name = "Test2FullNN"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"c__Dc_orders_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("C", "O", "L", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (customers, customerRef) = varset(Test2Full.name, "c", Test2Full.program(Test2Full.name).varRef.asInstanceOf[BagExpr])
  val (orders, orderRef) = varset("orders", "o", BagProject(customerRef, "c_orders"))
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val query = 
  ForeachUnion(customerRef, customers,
    projectTuple(customerRef, "c_orders" -> 
      ForeachUnion(orderRef, BagProject(customerRef, "c_orders"),
        projectTuple(orderRef, "o_parts" ->
          ReduceByKey(ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
            ForeachUnion(pr, relP,
              IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
                Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
          List("p_name"), List("l_quantity"))))))
  val program = Program(Assignment(name, query))
}

object Test3NN extends TPCHBase {

  val name = "Test3NN"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"n__Dn_custs_1", s"c__Dc_orders_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("C", "O", "L", "N", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (nations, nationRef) = varset(Test3Full.name, "n", Test3Full.program(Test3Full.name).varRef.asInstanceOf[BagExpr])
  val (customers, customerRef) = varset("customers", "c", BagProject(nationRef, "n_custs"))
  val (orders, orderRef) = varset("orders", "o", BagProject(customerRef, "c_orders"))
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val query = 
  ForeachUnion(nationRef, nations,
    Singleton(Tuple("n_name" -> nationRef("n_name"), "n_custs" ->
      ForeachUnion(customerRef, BagProject(nationRef, "n_custs"),
        Singleton(Tuple("c_name" -> customerRef("c_name"), "c_orders" -> 
          ForeachUnion(orderRef, BagProject(customerRef, "c_orders"),
            Singleton(Tuple("o_orderdate" -> orderRef("o_orderdate"), "o_parts" ->
              ReduceByKey(ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
                ForeachUnion(pr, relP,
                  IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
                    Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
                List("p_name"), List("l_quantity")))))))))))
  val program = Program(Assignment(name, query))
}

object Test3FullNN extends TPCHBase {

  val name = "Test3FullNN"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"n__Dn_custs_1", s"c__Dc_orders_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("C", "O", "L", "N", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (nations, nationRef) = varset(Test3Full.name, "n", Test3Full.program(Test3Full.name).varRef.asInstanceOf[BagExpr])
  val (customers, customerRef) = varset("customers", "c", BagProject(nationRef, "n_custs"))
  val (orders, orderRef) = varset("orders", "o", BagProject(customerRef, "c_orders"))
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val query = 
  ForeachUnion(nationRef, nations,
    projectTuple(nationRef, "n_custs" ->
      ForeachUnion(customerRef, BagProject(nationRef, "n_custs"),
        projectTuple(customerRef, "c_orders" -> 
          ForeachUnion(orderRef, BagProject(customerRef, "c_orders"),
            projectTuple(orderRef, "o_parts" ->
              ReduceByKey(ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
                ForeachUnion(pr, relP,
                  IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
                    Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
              List("p_name"), List("l_quantity"))))))))
  val program = Program(Assignment(name, query))
}

object Test4NN extends TPCHBase {

  val name = "Test4NN"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"r__Dr_nations_1", s"n__Dn_custs_1", s"c__Dc_orders_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("C", "O", "L", "N", "R", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (regions, regionRef) = varset(Test4Full.name, "r", Test4Full.program(Test4Full.name).varRef.asInstanceOf[BagExpr])
  val (nations, nationRef) = varset("nations", "n", BagProject(regionRef, "r_nations"))
  val (customers, customerRef) = varset("customers", "c", BagProject(nationRef, "n_custs"))
  val (orders, orderRef) = varset("orders", "o", BagProject(customerRef, "c_orders"))
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val query = 
  ForeachUnion(regionRef, regions,
    Singleton(Tuple("r_name" -> regionRef("r_name"), "r_nations" ->
      ForeachUnion(nationRef, BagProject(regionRef, "r_nations"),
        Singleton(Tuple("n_name" -> nationRef("n_name"), "n_custs" ->
          ForeachUnion(customerRef, BagProject(nationRef, "n_custs"),
            Singleton(Tuple("c_name" -> customerRef("c_name"), "c_orders" -> 
              ForeachUnion(orderRef, BagProject(customerRef, "c_orders"),
                Singleton(Tuple("o_orderdate" -> orderRef("o_orderdate"), "o_parts" ->
                  ReduceByKey(ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
                    ForeachUnion(pr, relP,
                      IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
                        Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
                  List("p_name"), List("l_quantity"))))))))))))))
  val program = Program(Assignment(name, query))
}

object Test4FullNN extends TPCHBase {

  val name = "Test4FullNN"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"r__Dr_nations_1", s"n__Dn_custs_1", s"c__Dc_orders_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("C", "O", "L", "N", "R", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (regions, regionRef) = varset(Test4Full.name, "r", Test4Full.program(Test4Full.name).varRef.asInstanceOf[BagExpr])
  val (nations, nationRef) = varset("nations", "n", BagProject(regionRef, "r_nations"))
  val (customers, customerRef) = varset("customers", "c", BagProject(nationRef, "n_custs"))
  val (orders, orderRef) = varset("orders", "o", BagProject(customerRef, "c_orders"))
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val query = 
  ForeachUnion(regionRef, regions,
    projectTuple(regionRef, "r_nations" ->
      ForeachUnion(nationRef, BagProject(regionRef, "r_nations"),
        projectTuple(nationRef, "n_custs" ->
          ForeachUnion(customerRef, BagProject(nationRef, "n_custs"),
            projectTuple(customerRef, "c_orders" -> 
              ForeachUnion(orderRef, BagProject(customerRef, "c_orders"),
                projectTuple(orderRef, "o_parts" ->
                  ReduceByKey(ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
                    ForeachUnion(pr, relP,
                      IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
                        Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
                  List("p_name"), List("l_quantity"))))))))))
  val program = Program(Assignment(name, query))
}

/** Skew experiment - takes input that has only a few attributes **/

object Test2NNL extends TPCHBase {

  val name = "Test2NNL"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"c__Dc_orders_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("C", "O", "L", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (customers, customerRef) = varset(Test2.name, "c", Test2.program(Test2.name).varRef.asInstanceOf[BagExpr])
  val (orders, orderRef) = varset("orders", "o", BagProject(customerRef, "c_orders"))
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val query = 
  ForeachUnion(customerRef, customers,
    Singleton(Tuple("c_name" -> customerRef("c_name"), "c_orders" -> 
      ForeachUnion(orderRef, BagProject(customerRef, "c_orders"),
        Singleton(Tuple("o_orderdate" -> orderRef("o_orderdate"), "o_parts" ->
          ReduceByKey(ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
            ForeachUnion(pr, relP,
              IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
                Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
          List("p_name"), List("l_quantity"))))))))

  val program = Program(Assignment(name, query))
}

object Test2FullNNL extends TPCHBase {

  val name = "Test2FullNNL"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"c__Dc_orders_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("C", "O", "L", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (customers, customerRef) = varset(Test2.name, "c", Test2.program(Test2.name).varRef.asInstanceOf[BagExpr])
  val (orders, orderRef) = varset("orders", "o", BagProject(customerRef, "c_orders"))
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val query = 
  ForeachUnion(customerRef, customers,
    projectTuple(customerRef, "c_orders" -> 
      ForeachUnion(orderRef, BagProject(customerRef, "c_orders"),
        projectTuple(orderRef, "o_parts" ->
          ReduceByKey(ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
            ForeachUnion(pr, relP,
              IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
                Singleton(Tuple("p_name" -> pr("p_name"), "l_quantity" -> partRef("l_quantity")))))),
          List("p_name"), List("l_quantity"))))))
  val program = Program(Assignment(name, query))
}

/** Nested to Nested with Aggregation **/

object Test2Agg extends TPCHBase {

  val name = "Test2Agg"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"c__Dc_orders_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("C", "O", "L", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (customers, customerRef) = varset(Test2Full.name, "c", Test2Full.program(Test2Full.name).varRef.asInstanceOf[BagExpr])
  val (orders, orderRef) = varset("orders", "o", BagProject(customerRef, "c_orders"))
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val red1 = ReduceByKey(ForeachUnion(orderRef, BagProject(customerRef, "c_orders"),
      ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
        ForeachUnion(pr, relP,
          IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
            Singleton(Tuple("p_name" -> pr("p_name"), "total" -> partRef("l_quantity").asNumeric * pr("p_retailprice").asNumeric)))))),
      List("p_name"), List("total"))

  val query = ForeachUnion(customerRef, customers,
      Singleton(Tuple("c_name" -> customerRef("c_name"), "totals" -> 
        ReduceByKey(ForeachUnion(orderRef, BagProject(customerRef, "c_orders"),
      ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
        ForeachUnion(pr, relP,
          IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
            Singleton(Tuple("p_name" -> pr("p_name"), "total" -> partRef("l_quantity").asNumeric * pr("p_retailprice").asNumeric)))))),
      List("p_name"), List("total")))))

  val program = Program(Assignment(name, query))
}

object Test2FullAgg extends TPCHBase {

  val name = "Test2FullAgg"
  override def indexedDict: List[String] = List(s"${name}__D_1", s"c__Dc_orders_1", s"o__Do_parts_1")

  def inputs(tmap: Map[String, String]): String = 
    s"val tpch = TPCHLoader(spark)\n${tmap.filter(x => List("C", "O", "L", "P").contains(x._1)).values.toList.mkString("")}"
 
  val (customers, customerRef) = varset(Test2Full.name, "c", Test2Full.program(Test2Full.name).varRef.asInstanceOf[BagExpr])
  val (orders, orderRef) = varset("orders", "o", BagProject(customerRef, "c_orders"))
  val (parts, partRef) = varset("parts", "l", BagProject(orderRef, "o_parts"))
  val query = 
  ForeachUnion(customerRef, customers,
    projectTuple(customerRef, "c_orders" -> 
      ReduceByKey(ForeachUnion(orderRef, BagProject(customerRef, "c_orders"),
          ForeachUnion(partRef, BagProject(orderRef, "o_parts"),
            ForeachUnion(pr, relP,
              IfThenElse(Cmp(OpEq, partRef("l_partkey"), pr("p_partkey")),
                Singleton(Tuple("p_name" -> pr("p_name"), "total" -> partRef("l_quantity").asNumeric * pr("p_retailprice").asNumeric)))))),
          List("p_name"), List("total"))))
  val program = Program(Assignment(name, query))
}

