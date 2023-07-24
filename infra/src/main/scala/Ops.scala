import scala.io.Source
import java.io.File

object Ops {
  
  def readFileIntoConfigMap(path: String, name: Option[String] = None): Map[String, String] = {
    val source = Source.fromFile(path)
    val content = try source.mkString finally source.close()
    val fileName = name.getOrElse(path.split("/").last)
    Map(fileName -> content)
  }
  
}
