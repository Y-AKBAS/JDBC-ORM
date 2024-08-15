
# Simple JDBC-ORM

This is a simplistic JDBC-ORM project that is built upon ***Spring Data JDBC***. The Project provides basically 2 Beans which are called
**`entityDao: EntityDao`** and **`paginationDao: PaginationDao`**. You can also define your own beans with different
dataSources and you can target as many database as you like with the entityDaos you create. The beautiful thing is
you do not need to define multiple Repository interfaces which are extending **`CrudRepository<E, ID>`** and you can just
simply use entityDao with the reified type information. Besides that you can define your entity as readOnly too.


Pros:
* You can very easily connect to the multiple databases and read write from/to them inside one application.
* If you use CrudRepositories then you need also Spring application context for testing but this is not needed for entityDao.
* Can be used in various scripts and doesn't require a fully fledged application
* Insertion operations are way faster than CrudRepository implementations.

Cons:
* Currently join operations are not supported and only mapping between database tables and classes are supported.
* For the time being it should be used with caution in complex scenarios as it is still in early development process.

### Example Usage

```kotlin
val localDbProperties = DBConnectionDetails(
    url = "jdbc:mysql://localhost:3306/customer_data",
    driverClassName = "com.mysql.cj.jdbc.Driver",
    userName = "root",
    password = "root"
)

val localEntityDao = EntityDao.create(localDbProperties, readOnly = false)

// Create
val insertUser = User(id = null, firstName = "Jack", lastName = "Joe", email = "jack@mail.com")
val dbUser = localEntityDao.insert(insertUser)
checkNotNull(dbUser.id) { "id for dbUser is not set" }

// Read
val readUser = localEntityDao.findByIdOrNull<User, Int>(id = dbUser.id)
requireNotNull(readUser) {"Found no user with id ${dbUser.id}"}

// Update
val isUpdated = localEntityDao.updateOne(readUser.copy(firstName = "updated name"))
check(isUpdated > 0) { "didn't update sth." }

// Delete
val isRemoved = localEntityDao.remove(readUser)
check(isRemoved > 0) { "didn't delete sth." }

