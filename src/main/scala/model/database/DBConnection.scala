package model.database

import slick.jdbc.HsqldbProfile.api._
import slick.jdbc.HsqldbProfile.backend.DatabaseDef

object DBConnection
{
    def get(test : Boolean = false) : DatabaseDef  =
    {
        connectionCache match
        {
            case Some(value) => value
            
            case None =>
            {
                val location = if(test) "/test" else ""
                val newConnection = Database.forURL(s"jdbc:hsqldb:file:database${location}/db", "SA", "", executor = AsyncExecutor.default("DbExecutor", 20))
                connectionCache = Some(newConnection)
                newConnection
            }
        }
    }
    
    private var connectionCache : Option[DatabaseDef] = None
}
