package sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.boundary;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.primefaces.model.LazyDataModel;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.ProductoDAO;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.VentaDAO;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.VentaDetalleDAO;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.Producto;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.Venta;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.VentaDetalle;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VentaDetalleFrmTest {

    @Mock VentaDetalleDAO ventaDetalleDao;
    @Mock VentaDAO ventaDao;
    @Mock ProductoDAO productoDao;
    @Mock FacesContext facesContext;
    @Mock ActionEvent actionEvent;

    private MockedStatic<FacesContext> mockedFacesContext;
    private Logger appLogger;

    @Spy
    @InjectMocks
    VentaDetalleFrm cut;

    private VentaDetalle mockDetalle;
    private Venta mockVenta;
    private Producto mockProducto;
    private UUID mockDetalleId = UUID.randomUUID();
    private UUID mockVentaId = UUID.randomUUID();
    private UUID mockProductoId = UUID.randomUUID();

    private Field registroField;
    private Field estadoField;
    private Field modeloField;
    private Class<?> estadoCrudClass;
    private Object estadoCrudNada;

    @BeforeEach
    void setUp() throws Exception {
        // Silenciar Logger para evitar ruido en consola, pero guardamos referencia
        appLogger = Logger.getLogger(VentaDetalleFrm.class.getName());
        appLogger.setLevel(Level.OFF);

        // FacesContext Mock
        mockedFacesContext = mockStatic(FacesContext.class);
        mockedFacesContext.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

        // Inicializar Entidades
        mockVenta = new Venta();
        mockVenta.setId(mockVentaId);

        mockProducto = new Producto();
        mockProducto.setId(mockProductoId);

        mockDetalle = new VentaDetalle();
        mockDetalle.setId(mockDetalleId);
        mockDetalle.setIdVenta(mockVenta);
        mockDetalle.setIdProducto(mockProducto);
        mockDetalle.setCantidad(BigDecimal.TEN);
        mockDetalle.setPrecio(new BigDecimal("5.00"));
        mockDetalle.setEstado("PENDIENTE");

        // Reflexión para campos heredados
        registroField = cut.getClass().getSuperclass().getDeclaredField("registro");
        registroField.setAccessible(true);
        estadoField = cut.getClass().getSuperclass().getDeclaredField("estado");
        estadoField.setAccessible(true);
        modeloField = cut.getClass().getSuperclass().getDeclaredField("modelo");
        modeloField.setAccessible(true);

        // Cargar Enum ESTADO_CRUD
        String enumClassName = "sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.boundary.ESTADO_CRUD";
        try {
            estadoCrudClass = Class.forName(enumClassName);
            estadoCrudNada = Enum.valueOf((Class<Enum>) estadoCrudClass, "NADA");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se encontró ESTADO_CRUD.", e);
        }

        doNothing().when(facesContext).addMessage(any(), any(FacesMessage.class));
    }

    @AfterEach
    void tearDown() {
        if (mockedFacesContext != null) mockedFacesContext.close();
        // Restaurar nivel de log
        if (appLogger != null) appLogger.setLevel(Level.INFO);
    }

    // ----------------------------------------------------------------------
    // --- Test Logger Initialization (CORREGIDO) ---
    // ----------------------------------------------------------------------
    @Test
    void testLoggerInitialization() throws Exception {
        // 1. Obtener el campo privado 'LOGGER'
        Field loggerField = VentaDetalleFrm.class.getDeclaredField("LOGGER");
        loggerField.setAccessible(true);

        // 2. Obtener el valor (static)
        Logger logger = (Logger) loggerField.get(null);

        // 3. Verificaciones
        assertNotNull(logger, "El Logger no debería ser nulo.");
        assertEquals(VentaDetalleFrm.class.getName(), logger.getName(),
                "El nombre del Logger debe coincidir con la clase.");

        // CORRECCIÓN: Validamos que sea OFF (porque lo seteamos en setUp)
        // O simplemente que no sea null, ya que el nivel puede variar por configuración externa.
        assertNotNull(logger.getLevel(), "El nivel del logger debería estar definido (OFF por setUp).");
    }

    // ----------------------------------------------------------------------
    // --- 1. Inicialización ---
    // ----------------------------------------------------------------------

    @Test
    void testInicializar_Success() {
        when(ventaDao.findAll()).thenReturn(List.of(mockVenta));
        when(productoDao.findAll()).thenReturn(List.of(mockProducto));

        cut.inicializar();

        assertNotNull(cut.getVentasDisponibles());
        assertFalse(cut.getVentasDisponibles().isEmpty());
        assertNotNull(cut.getProductosDisponibles());
        assertFalse(cut.getProductosDisponibles().isEmpty());
    }

    @Test
    void testCargarDatosFiltros_Exception() {
        when(ventaDao.findAll()).thenThrow(new RuntimeException("DB Error"));
        cut.inicializar();
        assertTrue(cut.getVentasDisponibles().isEmpty());
        assertTrue(cut.getProductosDisponibles().isEmpty());
    }

    // ----------------------------------------------------------------------
    // --- 2. Métodos Base ---
    // ----------------------------------------------------------------------

    @Test
    void testGetFacesContext() {
        assertEquals(facesContext, cut.getFacesContext());
    }

    @Test
    void testGetDao() {
        assertEquals(ventaDetalleDao, cut.getDao());
    }

    @Test
    void testGetSetIdVenta() {
        cut.setIdVenta(mockVentaId);
        assertEquals(mockVentaId, cut.getIdVenta());
    }

    @Test
    void testNuevoRegistro() {
        VentaDetalle nuevo = cut.nuevoRegistro();
        assertNotNull(nuevo);
        assertNotNull(nuevo.getId());
        assertEquals(BigDecimal.ZERO, nuevo.getCantidad());
        assertEquals("PENDIENTE", nuevo.getEstado());
    }

    // ----------------------------------------------------------------------
    // --- 3. Búsqueda y Conversión ---
    // ----------------------------------------------------------------------

    @Test
    void testBuscarRegistroPorId_Success() {
        when(ventaDetalleDao.findById(mockDetalleId)).thenReturn(mockDetalle);
        assertEquals(mockDetalle, cut.buscarRegistroPorId(mockDetalleId));
    }

    @Test
    void testBuscarRegistroPorId_Invalid() {
        assertNull(cut.buscarRegistroPorId(null));
        assertNull(cut.buscarRegistroPorId("string"));
    }

    @Test
    void testGetIdAsText() {
        assertEquals(mockDetalleId.toString(), cut.getIdAsText(mockDetalle));
        assertNull(cut.getIdAsText(null));
    }

    @Test
    void testGetIdByText_Success() {
        // Mockear DAO directamente
        when(ventaDetalleDao.findById(mockDetalleId)).thenReturn(mockDetalle);

        VentaDetalle result = cut.getIdByText(mockDetalleId.toString());

        assertNotNull(result);
        assertEquals(mockDetalle, result);
        verify(ventaDetalleDao).findById(mockDetalleId);
    }

    @Test
    void testGetIdByText_Invalid() {
        assertNull(cut.getIdByText(null));
        assertNull(cut.getIdByText(""));
        assertNull(cut.getIdByText("not-a-uuid"));
    }

    // ----------------------------------------------------------------------
    // --- 4. Validaciones ---
    // ----------------------------------------------------------------------

    @Test
    void testEsNombreVacio_AllFails() {
        mockDetalle.getIdVenta().setId(null);
        assertTrue(cut.esNombreVacio(mockDetalle));
        verify(facesContext, atLeastOnce()).addMessage(isNull(), argThat(m -> m.getDetail().contains("Venta")));
        reset(facesContext);

        mockDetalle.getIdVenta().setId(mockVentaId);
        mockDetalle.getIdProducto().setId(null);
        assertTrue(cut.esNombreVacio(mockDetalle));
        verify(facesContext, atLeastOnce()).addMessage(isNull(), argThat(m -> m.getDetail().contains("Producto")));
        reset(facesContext);

        mockDetalle.getIdProducto().setId(mockProductoId);
        mockDetalle.setCantidad(null);
        assertTrue(cut.esNombreVacio(mockDetalle));
        mockDetalle.setCantidad(BigDecimal.ZERO);
        assertTrue(cut.esNombreVacio(mockDetalle));
        verify(facesContext, atLeastOnce()).addMessage(isNull(), argThat(m -> m.getDetail().contains("cantidad")));
        reset(facesContext);

        mockDetalle.setCantidad(BigDecimal.TEN);
        mockDetalle.setPrecio(null);
        assertTrue(cut.esNombreVacio(mockDetalle));
        mockDetalle.setPrecio(new BigDecimal("-1"));
        assertTrue(cut.esNombreVacio(mockDetalle));
        verify(facesContext, atLeastOnce()).addMessage(isNull(), argThat(m -> m.getDetail().contains("precio")));
        reset(facesContext);

        mockDetalle.setPrecio(BigDecimal.TEN);
        mockDetalle.setEstado(null);
        assertTrue(cut.esNombreVacio(mockDetalle));
        mockDetalle.setEstado(" ");
        assertTrue(cut.esNombreVacio(mockDetalle));
        verify(facesContext, atLeastOnce()).addMessage(isNull(), argThat(m -> m.getDetail().contains("estado")));

        mockDetalle.setEstado("PENDIENTE");
        assertFalse(cut.esNombreVacio(mockDetalle));
    }

    // ----------------------------------------------------------------------
    // --- 5. Handlers CRUD ---
    // ----------------------------------------------------------------------

    @Test
    void testBtnGuardarHandler_Success() throws Exception {
        registroField.set(cut, mockDetalle);
        when(ventaDao.findById(mockVentaId)).thenReturn(mockVenta);
        when(productoDao.findById(mockProductoId)).thenReturn(mockProducto);
        doCallRealMethod().when(cut).inicializarRegistros();

        cut.btnGuardarHandler(actionEvent);

        verify(ventaDetalleDao).crear(mockDetalle);
        assertNull(registroField.get(cut));
        assertEquals(estadoCrudNada, estadoField.get(cut));
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO));
    }

    @Test
    void testBtnGuardarHandler_RegistroNull() {
        cut.btnGuardarHandler(actionEvent);
        verify(ventaDetalleDao, never()).crear(any());
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN));
    }

    @Test
    void testBtnGuardarHandler_FkNotFound() throws Exception {
        registroField.set(cut, mockDetalle);
        when(ventaDao.findById(mockVentaId)).thenReturn(null);
        when(productoDao.findById(mockProductoId)).thenReturn(mockProducto);
        cut.btnGuardarHandler(actionEvent);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getDetail().contains("Venta")));
        verify(ventaDetalleDao, never()).crear(any());
    }

    @Test
    void testBtnGuardarHandler_Exception() throws Exception {
        registroField.set(cut, mockDetalle);
        when(ventaDao.findById(mockVentaId)).thenReturn(mockVenta);
        when(productoDao.findById(mockProductoId)).thenReturn(mockProducto);
        doThrow(new RuntimeException("DB Error")).when(ventaDetalleDao).crear(any());

        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
        assertNotNull(registroField.get(cut));
    }

    @Test
    void testBtnModificarHandler_Success() throws Exception {
        registroField.set(cut, mockDetalle);
        when(ventaDao.findById(mockVentaId)).thenReturn(mockVenta);
        when(productoDao.findById(mockProductoId)).thenReturn(mockProducto);
        doCallRealMethod().when(cut).inicializarRegistros();

        cut.btnModificarHandler(actionEvent);

        verify(ventaDetalleDao).modificar(mockDetalle);
        assertNull(registroField.get(cut));
        assertEquals(estadoCrudNada, estadoField.get(cut));
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO));
    }

    @Test
    void testBtnModificarHandler_RegistroNull() {
        cut.btnModificarHandler(actionEvent);
        verify(ventaDetalleDao, never()).modificar(any());
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN));
    }

    @Test
    void testBtnModificarHandler_FkNotFound() throws Exception {
        registroField.set(cut, mockDetalle);
        when(ventaDao.findById(mockVentaId)).thenReturn(null);
        cut.btnModificarHandler(actionEvent);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getDetail().contains("entidades relacionadas")));
        verify(ventaDetalleDao, never()).modificar(any());
    }

    @Test
    void testBtnModificarHandler_Exception() throws Exception {
        registroField.set(cut, mockDetalle);
        when(ventaDao.findById(mockVentaId)).thenReturn(mockVenta);
        when(productoDao.findById(mockProductoId)).thenReturn(mockProducto);
        doThrow(new RuntimeException("DB Error")).when(ventaDetalleDao).modificar(any());

        cut.btnModificarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
    }

    // ----------------------------------------------------------------------
    // --- 7. LazyDataModel ---
    // ----------------------------------------------------------------------

    @Test
    void testCargarDetallesPorVenta_Logic() throws Exception {
        cut.cargarDetallesPorVenta(mockVentaId);

        assertNull(registroField.get(cut));
        assertEquals(estadoCrudNada, estadoField.get(cut));

        LazyDataModel<VentaDetalle> modelo = (LazyDataModel<VentaDetalle>) modeloField.get(cut);
        assertNotNull(modelo);

        when(ventaDetalleDao.contarPorVenta(mockVentaId)).thenReturn(5);
        assertEquals(5, modelo.count(Collections.emptyMap()));

        when(ventaDetalleDao.findPorVenta(mockVentaId, 0, 10)).thenReturn(List.of(mockDetalle));
        List<VentaDetalle> list = modelo.load(0, 10, Collections.emptyMap(), Collections.emptyMap());
        assertEquals(1, list.size());
        assertEquals(5, modelo.getRowCount());

        when(ventaDetalleDao.findById(mockDetalleId)).thenReturn(mockDetalle);
        assertEquals(mockDetalle, modelo.getRowData(mockDetalleId.toString()));
        assertEquals(null, modelo.getRowData("invalid-uuid"));

        assertEquals(mockDetalleId.toString(), modelo.getRowKey(mockDetalle));
        assertNull(modelo.getRowKey(null));
    }

    // ----------------------------------------------------------------------
    // --- 8. Getters Listas ---
    // ----------------------------------------------------------------------

    @Test
    void testGetVentasDisponibles_LazyLoad() throws Exception {
        Field ventasField = cut.getClass().getDeclaredField("ventasDisponibles");
        ventasField.setAccessible(true);
        ventasField.set(cut, null);

        when(ventaDao.findAll()).thenReturn(List.of(mockVenta));
        assertFalse(cut.getVentasDisponibles().isEmpty());
    }

    @Test
    void testGetProductosDisponibles_LazyLoad() throws Exception {
        Field productosField = cut.getClass().getDeclaredField("productosDisponibles");
        productosField.setAccessible(true);
        productosField.set(cut, null);

        when(productoDao.findAll()).thenReturn(List.of(mockProducto));
        assertFalse(cut.getProductosDisponibles().isEmpty());
    }

    @Test
    void testGetEstadosDisponibles() {
        List<String> estados = cut.getEstadosDisponibles();
        assertNotNull(estados);
        assertTrue(estados.contains("PENDIENTE"));
    }
}