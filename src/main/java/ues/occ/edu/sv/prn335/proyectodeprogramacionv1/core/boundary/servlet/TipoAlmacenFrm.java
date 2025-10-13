package ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.boundary.servlet;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.control.TipoAlmacenDAO;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.entity.TipoAlmacen;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class TipoAlmacenFrm implements Serializable {

    @Inject
    TipoAlmacenDAO taDao;

    private List<TipoAlmacen> listaTipoAlmacen;
    private String nombreBean = "Tipo de Almacén";
    private TipoAlmacen registro = new TipoAlmacen();
    private boolean mostrarFormulario = false;
    private Integer proximoId; // Calculado dinámicamente

    @PostConstruct
    public void inicializar() {
        try {// Carga inicial de registros y cálculo del próximo ID disponible
            listaTipoAlmacen = taDao.listarTodos();

            calcularProximoId();
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error al cargar datos", e.getMessage()));
        }
    }

    private void calcularProximoId() {
        try {
            // Consulta el próximo ID sugerido desde la base de datos
            Integer id = taDao.obtenerProximoId();
            proximoId = (id != null && id > 0) ? id : 1;
        } catch (Exception e) {
            e.printStackTrace();
            proximoId = 1;
        }
    }

    public void btnGuardarHandler(ActionEvent event) {
        try {
            // Guarda el nuevo registro en la base de datos
            taDao.crear(registro);

            // Reinicia el formulario y actualiza la lista
            registro = new TipoAlmacen();
            listaTipoAlmacen = taDao.findRange(0, Integer.MAX_VALUE);
            calcularProximoId();
            mostrarFormulario = false;

            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Registro guardado correctamente"));
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error al guardar", e.getMessage()));
        }
    }

    public void btnModificarHandler(ActionEvent event) {
        if (this.registro == null || this.registro.getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No hay registro seleccionado para modificar"));
            return;
        }
        try {
            // Actualiza el registro existente
            taDao.modificar(registro);

            // Limpieza y actualización de estado
            registro = new TipoAlmacen();
            listaTipoAlmacen = taDao.findRange(0, Integer.MAX_VALUE);
            calcularProximoId();
            mostrarFormulario = false;

            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Registro modificado correctamente"));
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error al modificar", e.getMessage()));
        }
    }

    public void btnNuevoHandler(ActionEvent event) {
        // Prepara el formulario para crear un nuevo registro
        this.registro = new TipoAlmacen();
        this.mostrarFormulario = true;
    }

    public void btnEliminarHandler(TipoAlmacen registro) {
        try {
            if (registro != null) {
                // Elimina el registro seleccionado
                taDao.eliminar(registro);

                // Refresca la lista y el ID sugerido
                this.listaTipoAlmacen = taDao.findRange(0, Integer.MAX_VALUE);
                calcularProximoId();

                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Registro eliminado correctamente"));
            }
        } catch (IllegalArgumentException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo eliminar el registro: " + e.getMessage()));
        } catch (IllegalAccessException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error de acceso", "No tiene permisos para eliminar: " + e.getMessage()));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error inesperado", "Error al eliminar: " + e.getMessage()));
        }
    }

    public void validarNombre(FacesContext facesContext, UIComponent uiComponent, Object nombre) {
        // Validación de longitud y contenido del campo nombre
        if (nombre == null || nombre.toString().isEmpty()) {
            throw new ValidatorException(new FacesMessage("El nombre no puede estar vacío"));
        }
        String nom = nombre.toString().trim();
        if (nom.length() < 1 || nom.length() > 155) {
            throw new ValidatorException(new FacesMessage("El nombre debe tener entre 1 y 155 caracteres"));
        }
    }





    // Getters y Setters
    public String getNombreBean() {
        return nombreBean;
    }

    public void setNombreBean(String nombreBean) {
        this.nombreBean = nombreBean;
    }

    public List<TipoAlmacen> getListaTipoAlmacen() {
        return listaTipoAlmacen;
    }

    public void setListaTipoAlmacen(List<TipoAlmacen> listaTipoAlmacen) {
        this.listaTipoAlmacen = listaTipoAlmacen;
    }

    public TipoAlmacen getRegistro() {
        return registro;
    }

    public void setRegistro(TipoAlmacen registro) {
        this.registro = registro;
    }

    public boolean isMostrarFormulario() {
        return mostrarFormulario;
    }

    public void setMostrarFormulario(boolean mostrarFormulario) {
        this.mostrarFormulario = mostrarFormulario;
    }

    public Integer getProximoId() {
        return proximoId;
    }

}