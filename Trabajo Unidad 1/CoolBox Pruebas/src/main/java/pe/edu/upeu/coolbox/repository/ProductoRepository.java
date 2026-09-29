package pe.edu.upeu.coolbox.repository;

import pe.edu.upeu.coolbox.enums.TipoProducto;
import pe.edu.upeu.coolbox.model.Categoria;
import pe.edu.upeu.coolbox.model.Marca;
import pe.edu.upeu.coolbox.model.Producto;
import pe.edu.upeu.coolbox.model.UnidMedida;

import java.time.LocalDate;

public class ProductoRepository extends AbstractJpaRepository<Producto, Long>{
    private long sequence=1;
    @Override
    protected Long getId(Producto entity) {
        return entity.getIdProducto();
    }

    @Override
    protected void setId(Producto entity, Long id) {
        entity.setIdProducto(id);
    }

    @Override
    protected Long generateId() {
        return sequence++;
    }

    // Datos de ejemplo. Los IDs y nombres coinciden con los de MarcaRepository,
    // CategoriaRepository y UnidadMedidaRepository para que la tabla muestre los nombres.
    public void seedData() {
        if (findAll().isEmpty()) {
            Marca samsung = new Marca(1L, "Samsung");
            Marca lg      = new Marca(2L, "LG");
            Marca sony    = new Marca(3L, "Sony");
            Marca apple   = new Marca(5L, "Apple");

            Categoria celulares   = new Categoria(1L, "Celulares");
            Categoria televisores = new Categoria(2L, "Televisores");
            Categoria parlantes   = new Categoria(3L, "Parlantes");

            UnidMedida unidad = new UnidMedida(1L, "Unidad");

            save(Producto.builder().idProducto(generateId()).nombre("Galaxy S24")
                    .tipoProducto(TipoProducto.PRODUCTO).pu(3499.0).puold(3699.0).utilidad(15.0)
                    .stock(10.0).stockold(12.0).idCategoria(celulares).idMarca(samsung).idUnidad(unidad)
                    .fechaRegistro(LocalDate.of(2026, 9, 1)).build());
            save(Producto.builder().idProducto(generateId()).nombre("iPhone 15")
                    .tipoProducto(TipoProducto.PRODUCTO).pu(4299.0).puold(4599.0).utilidad(12.0)
                    .stock(8.0).stockold(8.0).idCategoria(celulares).idMarca(apple).idUnidad(unidad)
                    .fechaRegistro(LocalDate.of(2026, 9, 5)).build());
            save(Producto.builder().idProducto(generateId()).nombre("Smart TV OLED 55")
                    .tipoProducto(TipoProducto.PRODUCTO).pu(3899.0).puold(4199.0).utilidad(18.0)
                    .stock(5.0).stockold(6.0).idCategoria(televisores).idMarca(lg).idUnidad(unidad)
                    .fechaRegistro(LocalDate.of(2026, 9, 10)).build());
            save(Producto.builder().idProducto(generateId()).nombre("Parlante SRS-XB100")
                    .tipoProducto(TipoProducto.PRODUCTO).pu(249.0).puold(279.0).utilidad(20.0)
                    .stock(25.0).stockold(30.0).idCategoria(parlantes).idMarca(sony).idUnidad(unidad)
                    .fechaRegistro(LocalDate.of(2026, 9, 15)).build());
        }
    }

}
