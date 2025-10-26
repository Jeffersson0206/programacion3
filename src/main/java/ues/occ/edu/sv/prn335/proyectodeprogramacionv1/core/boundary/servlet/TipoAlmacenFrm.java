package ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.boundary.servlet;

import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.control.InventarioDefaultDataAccess;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.control.TipoAlmacenDAO;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.entity.TipoAlmacen;

import java.io.Serializable;

@Named
@ViewScoped
public class TipoAlmacenFrm extends DefaultFrm<TipoAlmacen> implements Serializable {

    @Inject
    FacesContext facesContext;

    @Inject
    TipoAlmacenDAO tipoAlmacenDAO;

    public TipoAlmacenFrm() {
        this.nombreBean = "Tipo de Almacén";
    }


    @Override
    protected FacesContext getFacesContext() {
        return facesContext;
    }

    @Override
    protected InventarioDefaultDataAccess<TipoAlmacen> getDao() {
        return tipoAlmacenDAO;
    }

    @Override
    protected TipoAlmacen nuevoRegistro() {
        TipoAlmacen tipoAlmacen = new TipoAlmacen();
        return tipoAlmacen;
    }


    @Override
    protected TipoAlmacen buscarRegistroPorId(Integer id) {
        if (id != null && id instanceof Integer buscado && this.modelo.getWrappedData().isEmpty()) {
            for (TipoAlmacen ta : (Iterable<TipoAlmacen>) tipoAlmacenDAO.findAll()) {
                if (ta.getId().equals(buscado)) {
                    return ta;
                }
            }
        }
        return null;
    }

    @Override
    protected String getIdAsText(TipoAlmacen r) {
        if (r != null && r.getId() != null) {
            return r.getId().toString();
        }
        return null;
    }

    @Override
    protected TipoAlmacen getIdByText(String id) {
        if (id != null && this.modelo != null && !this.modelo.getWrappedData().isEmpty()) {
            try {
                Integer buscado = Integer.parseInt(id);
                return this.modelo.getWrappedData().stream()
                        .filter(r -> r.getId() != null && r.getId().equals(buscado))
                        .findFirst()
                        .orElse(null);
            } catch (NumberFormatException e) {
                System.err.println("ID no es un número válido: " + id);
                return null;
            }
        }
        return null;
    }

}