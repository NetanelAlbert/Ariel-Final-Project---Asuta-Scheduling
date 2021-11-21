package work

import akka.event.LoggingAdapter

trait Work

case class WorkSuccess(work: Work, message : Option[String])(implicit m_logger : LoggingAdapter)
{
    m_logger.info(s"WorkSuccess(work.Class = ${work.getClass}, message = $message)")
    
}

case class WorkFailure(work: Work, cause : Option[Throwable], message : Option[String])(implicit m_logger : LoggingAdapter)
{
    m_logger.error(s"WorkFailure(work.Class = ${work.getClass}, cause = $cause, message = $message)")
    cause.foreach(_.printStackTrace())
}
