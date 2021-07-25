package model.DTOs

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution

import java.io.{ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob

object FormattingProtocols
{
    import spray.json.DefaultJsonProtocol._
    import slick.jdbc.HsqldbProfile.api._
    
    implicit val SurgeonStatisticsFormat = jsonFormat8(SurgeonStatistics.apply)
    
    implicit val EnumeratedIntegerDistributionMapper = MappedColumnType.base[EnumeratedIntegerDistribution, Blob](
        distribution =>
        {
            val b = new ByteArrayOutputStream
            val out = new ObjectOutputStream(b)
            out.writeObject(distribution)
            out.flush()
            new SerialBlob(b.toByteArray)
        },
        blob =>
        {
            val in = new ObjectInputStream(blob.getBinaryStream)
            in.readObject().asInstanceOf[EnumeratedIntegerDistribution]
        })
}
