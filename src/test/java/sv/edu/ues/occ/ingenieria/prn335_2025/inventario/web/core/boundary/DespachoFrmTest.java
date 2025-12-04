package sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.boundary;

import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.event.SelectEvent;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.VentaDAO;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.VentaDetalleDAO;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.Venta;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.VentaDetalle;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DespachoFrmTest {

    // --- Mocks de Dependencias ---
    @Mock VentaDAO ventaDAO;
    @Mock VentaDetalleDAO ventaDetalleDAO;
    @Mock DespachoKardexFrm despachoKardexFrm;
    @Mock VentaDetalleFrm ventaDetalleFrm;
    @Mock FacesContext facesContext;

    // --- Mocks Estáticos de Framework ---
    private MockedStatic<FacesContext> mockedFacesContext;
    private MockedStatic<Logger> mockedLogger;

    // --- Clase a Probar (Spy) ---
    @Spy
    @InjectMocks
    DespachoFrm cut;

    // --- Variables de Prueba y Control ---
    private Venta mockVenta;
    private Venta mockVentaModelo;
    private UUID mockVentaId = UUID.randomUUID();
    private UUID mockVentaModeloId = UUID.randomUUID();

    // Campos de reflexión para acceder a campos protegidos
    private Field estadoField;
    private Field registroField;
    private Field modeloField;
    private Class<?> estadoCrudClass;

    /** Helper para acceder al Enum ESTADO_CRUD. */
    private Object getEstadoCrud(String nombre) throws Exception {
        if (estadoCrudClass == null) {
            throw new IllegalStateException("ESTADO_CRUD Class no inicializada.");
        }
        return Enum.valueOf((Class<Enum>) estadoCrudClass, nombre);
    }

    /** Helper para setear campos protegidos (registro, estado, modelo) */
    private void setProtectedField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        // --- Configuración de Mocks de Sistema ---
        Logger appLogger = Logger.getLogger(DespachoFrm.class.getName());
        appLogger.setLevel(Level.OFF);
        mockedLogger = mockStatic(Logger.class);
        mockedLogger.when(() -> Logger.getLogger(anyString())).thenReturn(appLogger);
        mockedFacesContext = mockStatic(FacesContext.class);
        mockedFacesContext.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

        // --- Entidades de Prueba ---
        mockVenta = new Venta();
        mockVenta.setId(mockVentaId);
        mockVentaModelo = new Venta();
        mockVentaModelo.setId(mockVentaModeloId);

        // --- Inicialización de Reflexión de Campos ---
        estadoField = cut.getClass().getSuperclass().getDeclaredField("estado");
        estadoField.setAccessible(true);
        registroField = cut.getClass().getSuperclass().getDeclaredField("registro");
        registroField.setAccessible(true);
        modeloField = cut.getClass().getSuperclass().getDeclaredField("modelo");
        modeloField.setAccessible(true);

        // --- SOLUCIÓN DE REFLEXIÓN PARA CLASE INDEPENDIENTE ESTADO_CRUD ---
        String enumClassName = "sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.boundary.ESTADO_CRUD";

        try {
            estadoCrudClass = Class.forName(enumClassName);
        } catch (ClassNotFoundException e) {
            NoSuchFieldException reflectionException =
                    new NoSuchFieldException("El Enum ESTADO_CRUD no se encontró en el classpath. Verifique el paquete: " + enumClassName);
            reflectionException.initCause(e);
            throw reflectionException;
        }
    }

    @AfterEach
    void tearDown() {
        mockedFacesContext.close();
        mockedLogger.close();
    }

    // ----------------------------------------------------------------------
    // --- Pruebas de buscarRegistroPorId (Añade Cobertura del ID) ---
    // ----------------------------------------------------------------------

    @Test
    void testBuscarRegistroPorId_Success_UUIDInstance() throws Exception {
        when(ventaDAO.find(mockVentaId)).thenReturn(mockVenta);
        Venta result = cut.buscarRegistroPorId(mockVentaId);
        assertEquals(mockVenta, result);
        verify(ventaDAO).find(mockVentaId);
    }

    @Test
    void testBuscarRegistroPorId_Success_StringInstance() throws Exception {
        when(ventaDAO.find(mockVentaId)).thenReturn(mockVenta);
        Venta result = cut.buscarRegistroPorId(mockVentaId.toString());
        assertEquals(mockVenta, result);
        verify(ventaDAO).find(mockVentaId);
    }

    /**
     * Cubre la rama donde el modelo no está nulo, pero la lista GetWrappedData es nula o vacía.
     */
    @Test
    void testBuscarRegistroPorId_ModelExists_WrappedDataNullOrEmpty() throws Exception {
        // Caso 1: WrappedData es nulo
        Object mockListDataModelNull = mock(modeloField.getType());
        when(mockListDataModelNull.getClass().getMethod("getWrappedData").invoke(mockListDataModelNull)).thenReturn(null);
        setProtectedField(cut, "modelo", mockListDataModelNull);
        when(ventaDAO.find(mockVentaId)).thenReturn(mockVenta);
        assertNotNull(cut.buscarRegistroPorId(mockVentaId));
        verify(ventaDAO, times(1)).find(mockVentaId); // Debe buscar en la DB

        // Caso 2: WrappedData es una lista vacía
        Object mockListDataModelEmpty = mock(modeloField.getType());
        when(mockListDataModelEmpty.getClass().getMethod("getWrappedData").invoke(mockListDataModelEmpty)).thenReturn(Collections.emptyList());
        setProtectedField(cut, "modelo", mockListDataModelEmpty);
        when(ventaDAO.find(mockVentaId)).thenReturn(mockVenta);
        assertNotNull(cut.buscarRegistroPorId(mockVentaId));
        verify(ventaDAO, times(2)).find(mockVentaId); // Debe buscar en la DB (total 2 veces)
    }

    /**
     * Cubre la rama donde el ID no es un UUID válido.
     */
    @Test
    void testBuscarRegistroPorId_InvalidID_NullOrInvalidString() {
        assertNull(cut.buscarRegistroPorId(null));

        // Cubre la rama de IllegalArgumentException
        assertNull(cut.buscarRegistroPorId("not-a-uuid"));
        verify(ventaDAO, never()).find(any());
    }

    @Test
    void testBuscarRegistroPorId_Exception() throws Exception {
        when(ventaDAO.find(mockVentaId)).thenThrow(new RuntimeException("Error DB"));
        assertNull(cut.buscarRegistroPorId(mockVentaId));
        // Se asegura de que se haya intentado buscar en el DAO
        verify(ventaDAO).find(mockVentaId);
    }

    // ----------------------------------------------------------------------
    // --- Pruebas de getIdByText (Cobertura de ramas de búsqueda) ---
    // ----------------------------------------------------------------------

    /**
     * Cubre la rama donde el modelo es nulo (llama directamente a buscarRegistroPorId).
     */
    @Test
    void testGetIdByText_ModelNull_FallbackToDAO() throws Exception {
        setProtectedField(cut, "modelo", null);
        when(ventaDAO.find(mockVentaId)).thenReturn(mockVenta);

        Venta result = cut.getIdByText(mockVentaId.toString());

        assertEquals(mockVenta, result);
        verify(ventaDAO).find(mockVentaId);
    }

    /**
     * Cubre la rama donde el modelo existe, pero getWrappedData es nulo (llama a buscarRegistroPorId).
     */
    @Test
    void testGetIdByText_ModelExists_WrappedDataNull_FallbackToDAO() throws Exception {
        Object mockListDataModel = mock(modeloField.getType());
        when(mockListDataModel.getClass().getMethod("getWrappedData").invoke(mockListDataModel)).thenReturn(null);
        setProtectedField(cut, "modelo", mockListDataModel);

        when(ventaDAO.find(mockVentaId)).thenReturn(mockVenta);

        Venta result = cut.getIdByText(mockVentaId.toString());

        assertEquals(mockVenta, result);
        verify(ventaDAO).find(mockVentaId);
    }

    /**
     * Cubre la rama donde el ID no es un UUID válido.
     */
    @Test
    void testGetIdByText_InvalidID_NullOrInvalidString() {
        assertNull(cut.getIdByText(null));
        // Cubre la rama de IllegalArgumentException
        assertNull(cut.getIdByText("not-a-uuid"));
        verify(ventaDAO, never()).find(any());
    }

    /**
     * Cubre la rama donde se encuentra en el modelo local (uso del stream findFirst).
     */
    @Test
    void testGetIdByText_FoundInModel_Path1() throws Exception {
        Object mockListDataModel = mock(modeloField.getType());
        when(mockListDataModel.getClass().getMethod("getWrappedData").invoke(mockListDataModel)).thenReturn(List.of(mockVentaModelo));
        setProtectedField(cut, "modelo", mockListDataModel);

        Venta result = cut.getIdByText(mockVentaModeloId.toString());

        assertEquals(mockVentaModelo, result);
        verify(ventaDAO, never()).find(any());
    }

    /**
     * Cubre la rama donde NO se encuentra en el modelo local (uso del orElse(buscarRegistroPorId)).
     */
    @Test
    void testGetIdByText_NotFoundInModel_FallbackToDAO() throws Exception {
        Object mockListDataModel = mock(modeloField.getType());
        when(mockListDataModel.getClass().getMethod("getWrappedData").invoke(mockListDataModel)).thenReturn(List.of(mockVentaModelo));
        setProtectedField(cut, "modelo", mockListDataModel);

        when(ventaDAO.find(mockVentaId)).thenReturn(mockVenta); // Este es el que se buscará en la DB

        Venta result = cut.getIdByText(mockVentaId.toString());

        assertEquals(mockVenta, result);
        verify(ventaDAO).find(mockVentaId);
    }

    // ----------------------------------------------------------------------
    // --- Pruebas de Métodos de DefaultFrm (Abstractos) ---
    // ----------------------------------------------------------------------

    @Test
    void testGetDao() {
        assertEquals(ventaDAO, cut.getDao());
    }

    @Test
    void testNuevoRegistro() {
        Venta nuevaVenta = cut.nuevoRegistro();
        assertNotNull(nuevaVenta);
        assertNotNull(nuevaVenta.getId());
        assertNotNull(nuevaVenta.getFecha());
        assertEquals("PAGADA", nuevaVenta.getEstado());
    }

    @Test
    void testGetFacesContext() {
        assertEquals(facesContext, cut.getFacesContext());
    }

    @Test
    void testGetIdAsText_Success() {
        assertEquals(mockVentaId.toString(), cut.getIdAsText(mockVenta));
    }

    @Test
    void testGetIdAsText_NullOrIdNull() {
        assertNull(cut.getIdAsText(null));
        Venta ventaSinId = new Venta();
        ventaSinId.setId(null);
        assertNull(cut.getIdAsText(ventaSinId));
    }

    /**
     * Cubre el método simple que retorna false.
     */
    @Test
    void testEsNombreVacio() {
        assertFalse(cut.esNombreVacio(mockVenta));
        assertFalse(cut.esNombreVacio(null));
    }

    // ----------------------------------------------------------------------
    // --- Pruebas de selectionHandler ---
    // ----------------------------------------------------------------------

    @Test
    void testSelectionHandler_Success() throws Exception {
        @SuppressWarnings("unchecked")
        SelectEvent<Venta> selectEvent = mock(SelectEvent.class);
        when(selectEvent.getObject()).thenReturn(mockVenta);

        cut.selectionHandler(selectEvent);

        assertEquals(mockVenta, registroField.get(cut));
        assertEquals(getEstadoCrud("MODIFICAR"), estadoField.get(cut));

        verify(ventaDetalleFrm).cargarDetallesPorVenta(mockVentaId);
        verify(despachoKardexFrm).limpiar();
    }

    @Test
    void testSelectionHandler_VentaNull() throws Exception {
        @SuppressWarnings("unchecked")
        SelectEvent<Venta> selectEvent = mock(SelectEvent.class);
        when(selectEvent.getObject()).thenReturn(null);
        setProtectedField(cut, "registro", mockVentaModelo);

        cut.selectionHandler(selectEvent);

        assertEquals(mockVentaModelo, registroField.get(cut));
        verify(ventaDetalleFrm, never()).cargarDetallesPorVenta(any());
        verify(despachoKardexFrm, never()).limpiar();
    }

    @Test
    void testSelectionHandler_IdNull() throws Exception {
        @SuppressWarnings("unchecked")
        SelectEvent<Venta> selectEvent = mock(SelectEvent.class);
        Venta ventaSinId = new Venta();
        ventaSinId.setId(null);
        when(selectEvent.getObject()).thenReturn(ventaSinId);

        cut.selectionHandler(selectEvent);

        assertEquals(ventaSinId, registroField.get(cut));
        assertEquals(getEstadoCrud("MODIFICAR"), estadoField.get(cut));

        // No debe llamar a cargarDetallesPorVenta si el ID es nulo.
        verify(ventaDetalleFrm, never()).cargarDetallesPorVenta(any());
        verify(despachoKardexFrm).limpiar();
    }

    @Test
    void testSelectionHandler_DespachoKardexFrmNull() throws Exception {
        // Establecer despachoKardexFrm a null
        cut.despachoKardexFrm = null;

        @SuppressWarnings("unchecked")
        SelectEvent<Venta> selectEvent = mock(SelectEvent.class);
        when(selectEvent.getObject()).thenReturn(mockVenta);

        // El método debe ejecutarse sin excepción (cubre el if (despachoKardexFrm != null))
        cut.selectionHandler(selectEvent);

        // Verificaciones normales
        verify(ventaDetalleFrm).cargarDetallesPorVenta(mockVentaId);
    }


    // ----------------------------------------------------------------------
    // --- Pruebas de Getters y Carga de Datos ---
    // ----------------------------------------------------------------------

    @Test
    void testGetVentaDetalleFrm_RegistroExists() {
        cut.registro = mockVenta;
        VentaDetalleFrm resultado = cut.getVentaDetalleFrm();
        verify(ventaDetalleFrm).setIdVenta(mockVentaId);
        assertEquals(ventaDetalleFrm, resultado);
    }

    @Test
    void testGetVentaDetalleFrm_RegistroNull() {
        cut.registro = null;
        VentaDetalleFrm resultado = cut.getVentaDetalleFrm();
        verify(ventaDetalleFrm, never()).setIdVenta(any());
        assertEquals(ventaDetalleFrm, resultado);
    }

    @Test
    void testGetDetallesCompraSeleccionada_Success() {
        cut.registro = mockVenta;
        List<VentaDetalle> detalles = List.of(new VentaDetalle());
        when(ventaDetalleDAO.findByIdVenta(mockVentaId)).thenReturn(detalles);
        assertFalse(cut.getDetallesCompraSeleccionada().isEmpty());
        verify(ventaDetalleDAO).findByIdVenta(mockVentaId);
    }

    @Test
    void testGetDetallesCompraSeleccionada_RegistroNullOrIdNull() {
        cut.registro = null;
        assertTrue(cut.getDetallesCompraSeleccionada().isEmpty());

        cut.registro = new Venta();
        assertTrue(cut.getDetallesCompraSeleccionada().isEmpty());

        verify(ventaDetalleDAO, never()).findByIdVenta(any());
    }

    @Test
    void testGetDespachoKardexFrm() {
        assertEquals(despachoKardexFrm, cut.getDespachoKardexFrm());
    }

    @Test
    void testCargarDatos_Success() throws Exception {
        // Se asegura que cargue por el estado correcto (PENDIENTE)
        when(ventaDAO.findByEstado("PENDIENTE")).thenReturn(List.of(mockVenta));
        assertFalse(cut.cargarDatos(0, 10).isEmpty());
        verify(ventaDAO).findByEstado("PENDIENTE");
    }

    /**
     * Cubre la rama de excepción en cargarDatos.
     */
    @Test
    void testCargarDatos_Exception() throws Exception {
        when(ventaDAO.findByEstado(anyString())).thenThrow(new RuntimeException());
        assertTrue(cut.cargarDatos(0, 10).isEmpty());
        // Se asegura que se haya loggeado el error (aunque el mockLogger esté OFF)
        mockedLogger.verify(() -> Logger.getLogger(anyString()), times(1));
    }
}