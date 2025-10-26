package ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.control;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.entity.TipoProducto;

import java.io.Serializable;
@Stateless
@LocalBean
public class TipoProductoDAO extends InventarioDefaultDataAccess<TipoProducto> implements Serializable {
    @PersistenceContext(unitName = "inventarioPU")
    EntityManager em;

    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    public TipoProductoDAO() { super(TipoProducto.class); }
}
