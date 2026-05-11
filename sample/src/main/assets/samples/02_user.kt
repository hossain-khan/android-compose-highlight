data class User(val name: String, val age: Int)

fun List<User>.filter(minAge: Int): List<User> =
    filter { it.age >= minAge }

// Unicode: héllo wörld 🌍
val users = listOf(
    User("Alice", 30),
    User("Bob", 25),
)

val adults = users.filter(18)
println(adults)