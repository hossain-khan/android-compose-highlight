def fibonacci(n: int) -> int:
    # Returns the nth Fibonacci number
    if n <= 1:
        return n
    a, b = 0, 1
    for _ in range(n - 1):
        a, b = b, a + b
    return b

# Edge case: special chars  \t \n ' "
result = fibonacci(10)
print(f"Result: {result}")