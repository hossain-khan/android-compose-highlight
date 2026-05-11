package main

import (
    "context"
    "fmt"
    "sync"
    "time"
)

type Result struct {
    URL      string
    Duration time.Duration
    Err      error
}

func fetchAll(ctx context.Context, urls []string) []Result {
    var mu sync.Mutex
    results := make([]Result, 0, len(urls))
    var wg sync.WaitGroup

    for _, url := range urls {
        wg.Add(1)
        go func(u string) {
            defer wg.Done()
            start := time.Now()
            select {
            case <-ctx.Done():
                mu.Lock()
                results = append(results, Result{URL: u, Err: ctx.Err()})
                mu.Unlock()
            case <-time.After(50 * time.Millisecond):
                mu.Lock()
                results = append(results, Result{URL: u, Duration: time.Since(start)})
                mu.Unlock()
            }
        }(url)
    }
    wg.Wait()
    return results
}

func main() {
    ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
    defer cancel()
    urls := []string{"https://example.com", "https://golang.org", "https://pkg.go.dev"}
    for _, r := range fetchAll(ctx, urls) {
        if r.Err != nil {
            fmt.Printf("ERR  %s: %v\n", r.URL, r.Err)
        } else {
            fmt.Printf("OK   %s  (%v)\n", r.URL, r.Duration)
        }
    }
}