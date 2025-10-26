package ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.boundary.servlet;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.control.InventarioDefaultDataAccess;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
public abstract class DefaultFrm<T> implements Serializable {
    ESTADO_CRUD estado = ESTADO_CRUD.NADA;

    protected String nombreBean;

    protected abstract FacesContext getFacesContext();

    protected abstract InventarioDefaultDataAccess<T> getDao();

    protected LazyDataModel<T> modelo;

    protected T registro;

    protected abstract T nuevoRegistro();

    protected abstract T buscarRegistroPorId(Integer id);

    abstract protected String getIdAsText(T r);

    abstract protected T getIdByText(String id);
    @PostConstruct
    public void inicializar(){
        this.modelo=new LazyDataModel<T>() {
            @Override
            public String getRowKey(T registro) {
                if (registro != null) {
                    try {
                        return getIdAsText(registro);
                    } catch (Exception e) {
                        Logger.getLogger(DefaultFrm.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
                return null;
            }

            @Override
            public T getRowData(String rowKey) {
                if (rowKey != null) {
                    try {
                        return getIdByText(rowKey);
                    } catch (Exception e) {
                        Logger.getLogger(DefaultFrm.class.getName()).log(Level.SEVERE, null, e);
                    }

                }
                return null;
            }

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                try {
                    return getDao().count();
                } catch (Exception e) {
                    Logger.getLogger(DefaultFrm.class.getName()).log(Level.SEVERE, null, e);
                }
                return 0;
            }

            @Override
            public List<T> load(int first, int max, Map<String, SortMeta> map, Map<String, FilterMeta> map1) {
                try {
                    return getDao().findRange(first, max);
                } catch (Exception e) {
                    Logger.getLogger(DefaultFrm.class.getName()).log(Level.SEVERE, null, e);
                }
                return Collections.emptyList();
            }
        };
    }
    public void seleccionarHandler(SelectEvent<T> r) {
        if(r!=null) {
            this.estado = ESTADO_CRUD.MODIFICAR;
            this.registro = r.getObject();
            System.out.println("Registro:"+this.registro);
        }else {
            System.out.println("Seleccion invalida");
        }
    }

    public void btnNuevoHandler(ActionEvent actionEvent) {
        this.registro = nuevoRegistro();
        this.estado = ESTADO_CRUD.CREAR;
    }

    public void btnCancelarHandler(ActionEvent actionEvent) {
        this.registro = null;
        this.estado = ESTADO_CRUD.NADA;
    }
    public void btnGuardarHandler(ActionEvent actionEvent) {
        if(this.registro!=null) {
            try{
                if(esNombreVacio(this.registro)){
                    getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atención", "El nombre no debe de estar vacio "));return;
                }
                getDao().crear(this.registro);
                this.registro =null;
                this.estado=ESTADO_CRUD.NADA;
                this.modelo=null;
                inicializar();
                getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "El registro se guardo exitosamente"));return;
            }catch(Exception e){
                e.printStackTrace();
                getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,"Error", e.getMessage()));
            }
        }else{
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,"Atención", "No hay registro para guardar"));
        }
    }
    public void btnEliminarHandler(ActionEvent actionEvent) {
        if(this.registro!=null){
            try{
                getDao().eliminar(this.registro);
                this.registro=null;
                this.estado=ESTADO_CRUD.NADA;
                inicializar();
                getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "El registro se elimino exitosamente"));
            }catch(Exception e){
                e.printStackTrace();
                getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,"Error", e.getMessage()));
            }
        }else{
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,"Atención", "No hay registro para eliminar"));
        }
    }
    public void btnModificarHandler(ActionEvent actionEvent) {
        if(this.registro!=null){
            try{
                if(esNombreVacio(this.registro)){
                    getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atención", "El nombre no debe de estar vacio "));return;
                }
                getDao().modificar(this.registro);
                this.registro=null;
                this.estado=ESTADO_CRUD.NADA;
                this.modelo=null;
                inicializar();
                getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "El registro se modifico exitosamente"));return;
            }catch(Exception e){
                e.printStackTrace();
                getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,"Error", e.getMessage()));
            }
        }else{
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,"Atención", "No hay registro para modificar"));
        }
    }
    private boolean esNombreVacio(T registro){
        try{
            java.lang.reflect.Method metodoGetNombre =  registro.getClass().getMethod("getNombre");
            String nombre = (String) metodoGetNombre.invoke(registro);
            return nombre == null || nombre.trim().isEmpty();
        }catch(Exception e){
            System.err.println("❌ Error al validar nombre: " + e.getMessage());
            return true;
        }
    }
    public void selectionHandler(SelectEvent<T> r) {
        if (r != null) {
            this.registro = r.getObject();
            this.estado = ESTADO_CRUD.MODIFICAR;
            System.out.println("✅ Registro seleccionado: " + this.registro);
        } else {
            System.out.println("⚠ Evento de selección nulo");
        }
    }
    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    protected int pageSize = 8;

    public ESTADO_CRUD getEstado() {
        return estado;
    }

    public void setEstado(ESTADO_CRUD estado) {
        this.estado = estado;
    }

    public String getNombreBean() {
        return nombreBean;
    }

    public void setNombreBean(String nombreBean) {
        this.nombreBean = nombreBean;
    }

    public LazyDataModel<T> getModelo() {
        return modelo;
    }

    public void setModelo(LazyDataModel<T> modelo) {
        this.modelo = modelo;
    }
    public T getRegistro() {
        return registro;
    }

    public void setRegistro(T registro) {
        this.registro = registro;
    }

}