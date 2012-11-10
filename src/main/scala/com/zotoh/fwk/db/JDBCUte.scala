/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSE,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SEE THE LICENSE FOR THE SPECIFIC LANGUAGE GOVERNING PERMISSIONS
 * AND LIMITATIONS UNDER THE LICENSE.
 *
 * You should have received a copy of the Apache License
 * along with this distribution; if not, you may obtain a copy of the
 * License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ??*/

package com.zotoh.fwk.db

import java.io.{InputStream,OutputStream,IOException}
import java.math.{BigDecimal,BigInteger}
import java.sql.{Blob,PreparedStatement,ResultSet}
import java.sql.{ResultSetMetaData,SQLException}

import java.sql.{Time=>JSTime,Timestamp=>JSTStamp,Date=>JSDate}
import java.util.{Date=>JUDate}

import com.zotoh.fwk.io.{IOUte,XData}
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.{Logger,Nichts,FileUte,CoreImplicits}
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Nichts._
import java.sql.Types._

import com.zotoh.fwk.crypto.Password

/**
 * @author kenl
 *
 */
object JDBCUte extends CoreImplicits {

  private var _log = getLogger(classOf[JDBCUte])
  def tlog() = _log

  /**
   * @param z
   * @return
   */
  def toSqlType(z:Class[_]) = {
    if (z == classOf[Boolean]) { BOOLEAN }
    else if (z == classOf[String]) { VARCHAR }
    else if (z == classOf[Int]) { INTEGER }
    else if (z == classOf[Long]) { BIGINT }
    else if (z == classOf[Double]) { DOUBLE }
    else if (z == classOf[Float]) { FLOAT }
    else if (z == classOf[Array[Byte]]) { BINARY }
    else
    z match {
      case t:BigDecimal => DECIMAL
      case t:JSDate => DATE
      case t:JSTime => TIME
      case t:JSTStamp => TIMESTAMP
      case t:JUDate => DATE
      case _ =>
        throw new SQLException("JDBC Type not supported: " + z.getName())
    }
//    if (BigInteger.class==z) return java.sql.Types.DECIMAL ;
  }

  /**
   * @param rs
   * @param col
   * @param javaSqlType
   * @param target
   * @return
   */
  def getObject(rs:ResultSet, col:Int,
        javaSqlType:Int, target:Class[_]) = {
    val cval = javaSqlType match {
      case SMALLINT | TINYINT =>  sql_short(rs, col)
      case INTEGER =>  sql_int(rs, col)
      case BIGINT =>  sql_long(rs, col)
      case REAL | FLOAT =>  sql_float(rs, col)
      case DOUBLE =>  sql_double(rs, col)
      case NUMERIC | DECIMAL =>  sql_bigdec(rs, col)
      case BOOLEAN =>  sql_bool(rs, col)
      case TIME => sql_time(rs, col)
      case DATE =>  sql_date(rs, col)
      case TIMESTAMP =>  sql_timestamp(rs, col)
      case LONGVARCHAR | VARCHAR =>  sql_string(rs, col)
      case LONGVARBINARY => sql_stream(rs, col)
      case VARBINARY | BINARY =>  sql_bytes(rs, col)
      case BLOB =>  sql_blob(rs, col)
      case BIT =>  sql_bit(rs, col)
      case NULL =>  sql_null(rs, col)
      //case LONGNVARCHAR || NVARCHAR || CLOB =>
      case _ => sql_notimpl( rs.getMetaData(), col)
    }

    safe_coerce(cval, target)
  }

  private def sql_short(rs:ResultSet, col:Int):Any = {
    rs.getShort(col)
  }

  private def sql_int(rs:ResultSet, col:Int):Any = {
    rs.getInt(col)
  }

  private def sql_long(rs:ResultSet, col:Int):Any = {
    rs.getLong(col)
  }

  private def sql_float(rs:ResultSet, col:Int):Any = {
    rs.getFloat(col)
  }

  private def sql_double(rs:ResultSet, col:Int):Any = {
    rs.getDouble(col)
  }

  private def sql_bigdec(rs:ResultSet, col:Int):Any = {
    rs.getBigDecimal(col)
  }

  private def sql_bool(rs:ResultSet, col:Int):Any = {
    rs.getBoolean(col)
  }

  private def sql_time(rs:ResultSet, col:Int):Any = {
    rs.getTime(col)
  }

  private def sql_date(rs:ResultSet, col:Int):Any = {
    rs.getDate(col)
  }

  private def sql_timestamp(rs:ResultSet, col:Int):Any = {
    rs.getTimestamp(col)
  }

  private def sql_string(rs:ResultSet, col:Int):Any = {
    rs.getString(col)
  }

  private def sql_bit(rs:ResultSet, col:Int):Any = {
    rs.getByte(col)
  }

  private def sql_null(rs:ResultSet, col:Int):Any = {
    Nichts.NICHTS
  }

  private def sql_notimpl(rs:ResultSetMetaData, col:Int) {
    val ct = rs.getColumnType(col)
    val cn = rs.getColumnName(col)
    throw new SQLException("Unsupported SQL Type: " + ct + " for column: " + cn)
  }

  private def sql_clob(rs:ResultSet, col:Int) {
  }

  private def sql_blob(rs:ResultSet, col:Int):Any = {
    val b = rs.getBlob(col)
    if (b==null) null else sql_stream( b.getBinaryStream() )
  }

  private def sql_stream(rs:ResultSet, col:Int):Any = {
    sql_stream( rs.getBinaryStream(col) )
  }

  private def sql_stream(inp:InputStream) = {
    val t = newTempFile(true)
    try {
      using(inp) { (inp) =>
      using(t._2) { (os) =>
          copy(inp, os)
      }}
      new XData(t._1)
    } catch {
      case e => FileUte.delete(t._1); throw e
    }
  }

  private def sql_bytes(rs:ResultSet, col:Int):Any = {
    rs.getBytes(col)
  }

  //------------ coerce

  private def safe_coerce(cval:Any, target:Class[_]):Any = {
    if (target==null) { return if (cval==null) NICHTS else cval }
    if (cval == null ) { return NICHTS }
    if (classOf[BigDecimal] == target ||
        classOf[Number].isAssignableFrom(target)) {
      return num_coerce(cval, target)
    }
    if (target == classOf[Array[Byte]]) { return bytes_coerce(cval,target) }
    if (target == classOf[Boolean]) { return bool_coerce(cval, target) }
    if (target == classOf[String]) { return string_coerce(cval,target) }
    target match {
      case t:XData => return stream_coerce(cval,target)
      case t:JSTStamp => return tstamp_coerce(cval, target)
      case t:JSTime => return time_coerce(cval, target)
      case t:JSDate => return date_coerce(cval,target)
      case t:JUDate => return date_coerce(cval,target)
    }

    throw new SQLException("Cannot coerce coltype: " + cval.getClass() +
      " to target-class: " + target)
  }

  private def tstamp_coerce(cval:Any, target:Class[_]):Any = {
    cval.getClass() match {
      case t:JSTStamp => cval
      case z =>
        throw new IOException("Cannot convert coltype: " +
            z + " to sql.timestamp")
    }
  }

  private def time_coerce(cval:Any, target:Class[_]):Any = {
    cval match {
      case t:JSTime => cval
      case t:JUDate =>
        new JSTime( cval.asInstanceOf[JUDate].getTime())
      case _ =>
        throw new IOException("Cannot convert coltype: " +
            cval.getClass() + " to sql.time")
    }
  }

  private def date_coerce(cval:Any, target:Class[_]):Any = {
    val dt= cval.asInstanceOf[JUDate]
    val z= cval.getClass()
    target match {
      case t:JSDate => if (z == classOf[JSDate] ) { cval } else  { new JSDate(dt.getTime()) }
      case t:JUDate => dt
      case _ =>
        throw new IOException("Cannot convert coltype: " +
          z + " to Date")
    }
  }

  private def string_coerce(cval:Any, target:Class[_]):Any = {
    cval.toString()
  }

  private def stream_coerce(cval:Any, target:Class[_]):Any =  {
    cval match {
      case v:Array[Byte] => new XData(v)
      case v:XData => v
      case _ =>
        throw new IOException("Cannot convert coltype: " +
              cval.getClass() + " to byte[]")
    }
  }

  private def bytes_coerce(cval:Any, target:Class[_]):Any = {
    cval match {
      case v:XData => v.bytes()
      case v:Array[Byte] => v
      case _ =>
        throw new IOException("Cannot convert coltype: " +
              cval.getClass() + " to byte[]")
    }
  }

  private def bool_coerce(cval:Any, target:Class[_]):Any = {
    cval match {
      case v:BigDecimal => v.intValue() > 0
      case v:Boolean => v
      case v:Number => v.intValue() > 0
      case _ =>
        throw new IOException("Cannot convert coltype: " +
            cval.getClass() + " to boolean")
    }
  }

  private def num_coerce(cval:Any, target:Class[_]):Any = {
    var big:BigDecimal=null
    var b:Number=null
    var rc=cval
    cval match {
      case v:BigInteger => throw new SQLException("Don't support BigInteger class")
      case v:BigDecimal => big=v
      case v:Number => b=v
    }

    if (target == classOf[Double]) {
      rc= if(big==null) b.doubleValue() else big.doubleValue()
    }
    else if (target==classOf[Float]) {
      rc= if(big== null) b.floatValue() else big.floatValue()
    }
    else if (target==classOf[Long]) {
      rc= if(big==null) b.longValue() else big.longValue()
    }
    else if (target==classOf[Int]) {
      rc= if(big==null) b.intValue() else big.intValue()
    }
    else if (target== classOf[Short]) {
      rc= if(big==null) b.shortValue() else big.shortValue()
    }
    else if (target== classOf[Byte]) {
      rc= if(big==null) b.byteValue() else big.byteValue()
    }

    rc
  }

  // ------------------------- set --------------

  private def javeToSQLBoolean(obj:Any) = {
    obj match {
      case v:Number => v.intValue() > 0
      case v:Boolean => v
      case _ =>
        throw new SQLException("Invalid datatype, expect boolean")
    }
  }

  private def javeToSQLInt(obj:Any) = {
    obj match {
      case v:Boolean => if (v) 1 else 0
      case v:Number => v.intValue()
      case _ =>
        throw new SQLException("Invalid datatype, expect int/short")
    }
  }

  private def javeToSQLLong(obj:Any) = {
    obj match {
      case v:Boolean => if (v) 1L else 0L
      case v:Number => v.longValue()
      case _ =>
        throw new SQLException("Invalid datatype, expect long/int/short")
    }
  }

  private def javeToSQLDecimal(obj:Any) = {
    obj match {
      case v:BigDecimal => v
      case v:BigInteger =>
        throw new SQLException("Unsupport BigInteger value type")
      case v:Number => new BigDecimal( v.doubleValue() )
      case _ =>
        throw new SQLException("Invalid datatype, expect number type, got: " + obj.getClass() )
    }
  }

  private def javeToSQLDouble(obj:Any) = {
    obj match {
      case d:Double  => d.toDouble
      case f:Float => f.toDouble
      case _ =>
        throw new SQLException("Invalid datatype, expect double/float")
    }
  }

  private def javeToSQLFloat(obj:Any) = {
    obj match {
      case d:Double  => d.toFloat
      case f:Float => f.toFloat
      case _ =>
        throw new SQLException("Invalid datatype, expect double/float")
    }
  }

  private def javaToSQLDate(obj:Any) = {
    obj match {
      case d:JSDate => d
      case d:JUDate => new JSDate(d.getTime())
      case _ =>
        throw new SQLException("Invalid datatype, expect date")
    }
  }

  private def javaToSQLTime(obj:Any) = {
    obj match {
      case t:JSTime => t
      case t:JUDate => new JSTime( t.getTime() )
      case _ =>
        throw new SQLException("Invalid datatype, expect date/time")
    }
  }

  private def javaToSQLTimestamp(obj:Any) = {
    obj match {
      case t:JSTStamp => t
      case t:JUDate => new JSTStamp( t.getTime() )
      case _ =>
        throw new SQLException("Invalid datatype, expect date/timestamp")
    }
  }

  /**
   * @param stmt
   * @param pos
   * @param sqlType
   * @param value
   */
  def setStatement(stmt:PreparedStatement, pos:Int, sqlType:Int, value:Any) {

    var z:Class[_]= null

    if (isNichts(value)) { stmt.setNull(pos, sqlType) } else {
      z= value.getClass()
    }

    if (z != null) sqlType match {

      case BOOLEAN =>
        stmt.setBoolean(pos, javeToSQLBoolean(value))

        // numbers
      case DECIMAL | NUMERIC =>
        stmt.setBigDecimal(pos, javeToSQLDecimal(value))

        // ints
      case BIGINT =>
        stmt.setLong(pos, javeToSQLLong(value))

      case INTEGER | TINYINT | SMALLINT =>
        stmt.setInt(pos, javeToSQLInt(value))

        // real numbers
      case DOUBLE =>
        stmt.setDouble(pos, javeToSQLDouble(value))

      case REAL | FLOAT =>
        stmt.setFloat(pos, javeToSQLFloat(value))

        // date time
      case DATE =>
        stmt.setDate(pos, javaToSQLDate(value))

      case TIME =>
        stmt.setTime(pos, javaToSQLTime(value))

      case TIMESTAMP =>
        stmt.setTimestamp(pos, javaToSQLTimestamp(value))

        // byte[]
      case VARBINARY | BINARY =>
        var b:Array[Byte]=null
        z match {
          case t:XData => b= value.asInstanceOf[XData].bytes()
          case _ =>
            if (z==classOf[Array[Byte]]) { b= value.asInstanceOf[Array[Byte]] }
        }
        if (b==null) {
          throw new SQLException("Expecting byte[] , got : " + z)
        }
        stmt.setBytes(pos, b)

      case LONGNVARCHAR | CLOB | NVARCHAR =>
        throw new SQLException("Unsupported SQL type: " + sqlType)

        // strings
      case LONGVARCHAR | VARCHAR =>
        var s= value match {
          case pwd:Password =>  pwd.encoded()
          case str:String => str
          case _ => value.toString()
        }
        stmt.setString(pos, s)

      case LONGVARBINARY | BLOB =>
        var inp = value match {
          case v:Array[Byte] => asStream(v)
          case v:XData => v.stream()
          case _ => null
        }
        if (inp==null) {  throw new SQLException("Expecting byte[] , got : " + z) }
        stmt.setBinaryStream(pos, inp, inp.available() )
    }

  }

  /**
   * @param stmt
   * @param pos
   * @param value
   */
  def setStatement(stmt:PreparedStatement, pos:Int, value:Any) {
    if (value == null) {
      throw new SQLException("Unexpected null object")
    }
    val z= value.getClass()
    if (z==classOf[Boolean]) { stmt.setBoolean(pos, value.asInstanceOf[Boolean] ) }
    else if (z==classOf[Long]) { stmt.setLong(pos, value.asInstanceOf[Long] ) }
    else if (z== classOf[Int] ) { stmt.setInt(pos, value.asInstanceOf[Int] ) }
    else if (z== classOf[Short]) { stmt.setShort(pos, value.asInstanceOf[Short] ) }
    else if (z== classOf[Double]) { stmt.setDouble(pos, value.asInstanceOf[Double] ) }
    else if (z== classOf[Float]) { stmt.setFloat(pos, value.asInstanceOf[Float] ) }
    else if (z== classOf[Array[Byte]]) { stmt.setBytes(pos, value.asInstanceOf[Array[Byte]]) }
    else if (z== classOf[String]) { stmt.setString(pos, value.toString()) }
    else z match {
      case t:BigDecimal => stmt.setBigDecimal(pos, value.asInstanceOf[BigDecimal] )
      case t:BigInteger => throw new SQLException("Don't support BigInteger class")
      case t:JSTStamp => stmt.setTimestamp(pos, value.asInstanceOf[JSTStamp] )
      case t:JSTime => stmt.setTime(pos, value.asInstanceOf[JSTime])
      case t:JSDate => stmt.setDate(pos, value.asInstanceOf[JSDate])
      case t:JUDate => stmt.setDate(pos, new JSDate(value.asInstanceOf[JUDate].getTime() ))
      case t:Password => stmt.setString(pos, value.asInstanceOf[Password].encoded())
      case t:XData =>
        var d= value.asInstanceOf[XData]
        if (d.isDiskFile()) {
          stmt.setBinaryStream(pos, d.stream() )
        } else {
          stmt.setBytes(pos,  d.bytes())
        }
      case t:Nichts =>
        stmt.setObject(pos, null)
      case _ => throw new SQLException("Unsupport value class: " + z)
    }

  }

}

sealed class JDBCUte {}


