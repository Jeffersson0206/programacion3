package ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.boundary.servlet;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.control.InventarioDefaultDataAccess;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.control.TipoProductoDAO;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.entity.TipoProducto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class TipoProductoFrm extends DefaultFrm<TipoProducto> implements Serializable {
    @Inject
    FacesContext facesContext;
    @Inject
    TipoProductoDAO tipoProductoDao;

    private Long tipoProductoPadreSeleccionado;
    private List<SelectItem> tiposProductoHierarchy;
    public TipoProductoFrm() {
        this.nombreBean = "Tipo de Producto";
    }
    @Override
    protected FacesContext getFacesContext() {
        return facesContext;
    }

    @Override
    protected InventarioDefaultDataAccess<TipoProducto> getDao() {
        return tipoProductoDao;
    }

    @Override
    protected TipoProducto nuevoRegistro() {
        TipoProducto tp = new TipoProducto();
        tp.setActivo(true);
        tipoProductoPadreSeleccionado = null;
        return tp;
    }

    @Override
    protected TipoProducto buscarRegistroPorId(Integer id) {
        if (id != null) {
            for (TipoProducto tp : tipoProductoDao.findAll()) {
                if (tp.getId().equals(id)) {
                    return tp;
                }
            }
        }
        return null;
    }

    @Override
    protected String getIdAsText(TipoProducto r) {
        if (r != null && r.getId() != null) {
            return r.getId().toString();
        }
        return null;
    }

    @Override
    protected TipoProducto getIdByText(String id) {
        if (id != null && this.modelo != null && !this.modelo.getWrappedData().isEmpty()) {
            try {
                Long buscado = Long.parseLong(id);
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
    private void cargarTiposProductoHierarchy() {
        try {
            tiposProductoHierarchy = new ArrayList<>();
            List<TipoProducto> todosTipos = (List<TipoProducto>) tipoProductoDao.findAll();

            if (todosTipos == null || todosTipos.isEmpty()) {
                System.out.println("⚠️ No hay tipos de producto en la base de datos");
                return;
            }

            // Filtrar SOLO los tipos que NO tienen padre (tipos raíz)
            for (TipoProducto tp : todosTipos) {
                if (tp.getIdTipoProductoPadre() == null) {
                    tiposProductoHierarchy.add(new SelectItem(tp.getId(), tp.getNombre()));
                    System.out.println("✅ Tipo raíz agregado: " + tp.getNombre() + " (ID: " + tp.getId() + ")");
                } else {
                    System.out.println("⏭️  Ignorado (tiene padre): " + tp.getNombre()
                            + " -> Padre ID: " + tp.getIdTipoProductoPadre().getId());
                }
            }

            System.out.println("📊 Total de tipos raíz cargados: " + tiposProductoHierarchy.size());

        } catch (Exception e) {
            System.err.println("❌ Error al cargar tipos de producto: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @Override
    public void btnGuardarHandler(ActionEvent actionEvent) {
        try {
            asignarTipoProductoPadre();
            super.btnGuardarHandler(actionEvent);
            cargarTiposProductoHierarchy();
            getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Registro guardado correctamente"));
        } catch (Exception e) {
            getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error al guardar", e.getMessage()));
        }
    }

    @Override
    public void btnModificarHandler(ActionEvent actionEvent) {
        try {
            asignarTipoProductoPadre();
            super.btnModificarHandler(actionEvent);
            cargarTiposProductoHierarchy();
            getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Registro modificado correctamente"));
        } catch (Exception e) {
            getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error al modificar", e.getMessage()));
        }
    }

    @Override
    public void btnCancelarHandler(ActionEvent actionEvent) {
        super.btnCancelarHandler(actionEvent);
        tipoProductoPadreSeleccionado = null;
    }

    @Override
    public void btnNuevoHandler(ActionEvent actionEvent) {
        tipoProductoPadreSeleccionado = null;
        tiposProductoHierarchy = null; // Limpiar cache
        cargarTiposProductoHierarchy();
        super.btnNuevoHandler(actionEvent);
    }

    /**
     * Sobrescribe selectionHandler para cargar el tipo padre en el selector
     */
    @Override
    public void selectionHandler(org.primefaces.event.SelectEvent<TipoProducto> r) {
        super.selectionHandler(r);
        if (this.registro != null && this.registro.getIdTipoProductoPadre() != null) {
            tipoProductoPadreSeleccionado = this.registro.getIdTipoProductoPadre().getId();
        } else {
            tipoProductoPadreSeleccionado = null;
        }
    }

    /**
     * Asigna el tipo padre seleccionado al registro
     */
    private void asignarTipoProductoPadre() {
        if (this.registro != null) {
            if (tipoProductoPadreSeleccionado != null && tipoProductoPadreSeleccionado > 0) {
                // Buscar el tipo padre en la lista de todos los tipos
                List<TipoProducto> todosTipos = (List<TipoProducto>) tipoProductoDao.findAll();
                TipoProducto tipoPadre = todosTipos.stream()
                        .filter(tp -> tp.getId().equals(tipoProductoPadreSeleccionado))
                        .findFirst()
                        .orElse(null);
                this.registro.setIdTipoProductoPadre(tipoPadre);
                System.out.println("✅ Tipo padre asignado: " + (tipoPadre != null ? tipoPadre.getNombre() : "ninguno"));
            } else {
                this.registro.setIdTipoProductoPadre(null);
                System.out.println("✅ Tipo padre limpiado (será tipo raíz)");
            }
        }
    }

    /**
     * Getter para los tipos de producto - siempre carga fresco
     */
    public List<SelectItem> getTiposProductoHierarchy() {
        if (tiposProductoHierarchy == null) {
            cargarTiposProductoHierarchy();
        }
        return tiposProductoHierarchy;
    }

    public Long getTipoProductoPadreSeleccionado() {
        return tipoProductoPadreSeleccionado;
    }

    public void setTipoProductoPadreSeleccionado(Long tipoProductoPadreSeleccionado) {
        this.tipoProductoPadreSeleccionado = tipoProductoPadreSeleccionado;
    }
}
