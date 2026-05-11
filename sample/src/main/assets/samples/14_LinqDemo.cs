using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

public record Product(int Id, string Name, decimal Price, int Stock);

public class ProductService(IEnumerable<Product> catalog)
{
    public IEnumerable<Product> Search(string query, decimal? maxPrice = null) =>
        catalog
            .Where(p => p.Name.Contains(query, StringComparison.OrdinalIgnoreCase))
            .Where(p => maxPrice is null || p.Price <= maxPrice)
            .OrderBy(p => p.Price);

    public async Task<Product?> FindCheapestAsync(string query)
    {
        await Task.Delay(10); // simulate DB round-trip
        return Search(query).FirstOrDefault();
    }
}

var catalog = new List<Product>
{
    new(1, "Kotlin Book",    29.99m, 100),
    new(2, "Rust in Action", 34.99m,  50),
    new(3, "Go Programming", 24.99m,  75),
};

var svc = new ProductService(catalog);
var hit = await svc.FindCheapestAsync("kotlin");
Console.WriteLine(hit is { } p
    ? ${'$'}"Found: {p.Name} at ${'$'}{p.Price:C}"
    : "Not found");