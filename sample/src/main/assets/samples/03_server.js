async function fetchUser(id) {
    const response = await fetch(`/api/users/${'$'}{id}`);
    if (!response.ok) {
        throw new Error(`HTTP error: ${'$'}{response.status}`);
    }
    return response.json();
}

// Backslash path: C:\Users\test
const path = 'C:\\Users\\test\\file.txt';