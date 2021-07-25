package model.database

import slick.jdbc.HsqldbProfile.api._
import slick.jdbc.HsqldbProfile.backend.DatabaseDef

object DBConnection
{
    def get(test : Boolean = false) : DatabaseDef  =
    {
        val location = if(test) "/test" else ""
        Database.forURL(s"jdbc:hsqldb:file:database${location}/db", "SA", "", executor = AsyncExecutor.default("DbExecutor", 20))
    }
}
