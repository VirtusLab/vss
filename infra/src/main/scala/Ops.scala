import java.io.File
import scala.io.Source

object Ops {
  
  def readFileIntoConfigMap(path: String, name: Option[String] = None): Map[String, String] = {
    val source = Source.fromFile(path)
    val content = try source.mkString finally source.close()
    val fileName = name.getOrElse(path.split("/").last)
    Map(fileName -> content)
  }
  
}
