package test

import org.scalatest.Assertions._
import org.scalatest._

class TestSuite extends FunSuite with BeforeAndAfterEach
with BeforeAndAfterAll {

  override def beforeAll(configMap: Map[String, Any]) {
  }

  override def afterAll(configMap: Map[String, Any]) {
  }

  override def beforeEach() { }

  override def afterEach() { }

  test("testDummy") {
    println("test OK")
    assert(true)
  }

}
