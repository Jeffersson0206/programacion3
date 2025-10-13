package ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.control;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.entity.TipoAlmacen;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
@Stateless
@LocalBean
public class TipoAlmacenDAO extends InventarioDefaultDataAccess<TipoAlmacen> implements Serializable {
    public TipoAlmacenDAO() { super(TipoAlmacen.class); }
    @PersistenceContext(unitName = "inventarioPU")
    EntityManager em;
    @Override
    public EntityManager getEntityManager() {
        return em;
    }
    public List<TipoAlmacen> findRange(int first, int max) throws IllegalArgumentException {
        if (first < 0 || max < 1) {
            throw new IllegalArgumentException();
        }
        try {
            EntityManager em = getEntityManager();
            if (em != null) {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<TipoAlmacen> cq = cb.createQuery(TipoAlmacen.class);
                Root<TipoAlmacen> rootEntry = cq.from(TipoAlmacen.class);
                CriteriaQuery<TipoAlmacen> all = cq.select(rootEntry);
                TypedQuery<TipoAlmacen> allQuery = em.createQuery(all);
                allQuery.setFirstResult(first);
                allQuery.setMaxResults(max);
                return allQuery.getResultList();
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        throw new IllegalStateException("No se puede acceder al repositorio");
    }
    public void eliminar(TipoAlmacen registro) throws IllegalArgumentException, IllegalAccessException {
        if (registro == null || registro.getId() == null) {
            throw new IllegalArgumentException("El registro o su ID no pueden ser nulos");
        }
        try {
            EntityManager em = getEntityManager();
            if (em != null) {
                TipoAlmacen toRemove = em.find(TipoAlmacen.class, registro.getId());
                if (toRemove != null) {
                    em.remove(em.contains(toRemove) ? toRemove : em.merge(toRemove));
                } else {
                    throw new IllegalArgumentException("El registro no existe en la base de datos");
                }
            } else {
                throw new IllegalStateException("EntityManager no disponible");
            }
        } catch (IllegalArgumentException ext) {
            throw ext;
        } catch (Exception ex) {
            throw new IllegalStateException("Error al eliminar el registro", ex);
        }
    }
    public void crear(TipoAlmacen registro) throws IllegalArgumentException, IllegalAccessException {
        if (registro == null) {
            throw new IllegalArgumentException("El registro no puede ser nulo");
        }
        try {
            EntityManager em = getEntityManager();
            if (em != null) {
                em.persist(registro);
            } else {
                throw new IllegalStateException("EntityManager no disponible");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Error al crear el registro", ex);
        }
    }
    public void modificar(TipoAlmacen registro) throws IllegalArgumentException {
        if (registro == null) {
            throw new IllegalArgumentException("El registro no puede ser nulo");
        }
        try {
            EntityManager em = getEntityManager();
            if (em != null) {
                em.merge(registro);
            } else {
                throw new IllegalStateException("EntityManager no disponible");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Error al modificar el registro", ex);
        }
    }
    public Integer obtenerProximoId() {
        try {
            EntityManager em = getEntityManager();

            if (em != null) {
                // Obtener el máximo ID actual de la tabla
                Query maxQuery = em.createQuery(
                        "SELECT MAX(t.id) FROM TipoAlmacen t"
                );
                Integer maxId = (Integer) maxQuery.getSingleResult();
                System.out.println("DEBUG: Max ID actual = " + maxId);

                if (maxId != null && maxId > 0) {
                    return maxId + 1;
                }
            }
            return 1;
        } catch (Exception e) {
            System.out.println("DEBUG: Error obteniendo próximo ID: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
    public List<TipoAlmacen> listarTodos() {
        try {
            return em.createQuery("SELECT t FROM TipoAlmacen t", TipoAlmacen.class).getResultList();
        } catch (Exception e) {
            e.printStackTrace(); // o loguear el error
            return new ArrayList<>();
        }

    }

}
