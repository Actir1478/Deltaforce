package pe.edu.upeu.coolbox.repository;

import pe.edu.upeu.coolbox.model.Categoria;

public class CategoriaRepository extends AbstractJpaRepository<Categoria, Long>{
    private long sequence=1;

    @Override
    protected Long getId(Categoria entity) {
        return entity.getIdCategoria();
    }

    @Override
    protected void setId(Categoria entity, Long id) {
        entity.setIdCategoria(id);
    }

    @Override
    protected Long generateId() {
        return sequence++;
    }

    public void seedData() {
        if (findAll().isEmpty()) {
            save(new Categoria(generateId(), "Ceculares"));
            save(new Categoria(generateId(),"Televisores"));
            save(new Categoria(generateId(),"Parlantes"));
        }
    }


}
