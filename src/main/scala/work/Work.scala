package work

import akka.event.LoggingAdapter

trait Work

case class WorkSuccess(work: Work)

case class WorkFailure(work: Work, cause : Option[Throwable], message : Option[String])(implicit m_logger : LoggingAdapter)
{
    m_logger.error(toString)
}
