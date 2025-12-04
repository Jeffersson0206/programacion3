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
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.*;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
class KardexFrmTest {

    // --- Mocks ---
    @Mock KardexDAO kardexDAO;
    @Mock ProductoDAO productoDAO;
    @Mock AlmacenDAO almacenDAO;
    @Mock CompraDetalleDAO compraDetalleDAO;
    @Mock VentaDetalleDAO ventaDetalleDAO;
    @Mock FacesContext facesContext;
    @Mock ActionEvent actionEvent;

    // Configuración para simular FacesContext y Logger
    private MockedStatic<FacesContext> mockedFacesContext;
    private MockedStatic<Logger> mockedLogger;

    @Spy
    @InjectMocks
    KardexFrm cut;

    // --- Entidades y IDs ---
    private UUID mockKardexId = UUID.randomUUID();
    private UUID mockProductoId = UUID.randomUUID();
    private Integer mockAlmacenId = 1;
    private UUID mockCompraDetalleId = UUID.randomUUID();
    private UUID mockVentaDetalleId = UUID.randomUUID();

    private Kardex mockKardex;
    private Producto mockProducto;
    private Almacen mockAlmacen;
    private CompraDetalle mockCompraDetalle;
    private VentaDetalle mockVentaDetalle;

    // --- Reflexión ---
    private Field registroField;
    private Field estadoField;
    private Field productosDisponiblesField;
    private Field almacenesDisponiblesField;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Mocks Estáticos
        mockedFacesContext = mockStatic(FacesContext.class);
        mockedFacesContext.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
        doNothing().when(facesContext).addMessage(any(), any(FacesMessage.class));

        mockedLogger = mockStatic(Logger.class);
        mockedLogger.when(() -> Logger.getLogger(anyString())).thenReturn(mock(Logger.class));

        // 2. Configuración de Entidades
        mockProducto = new Producto();
        mockProducto.setId(mockProductoId);

        mockAlmacen = new Almacen();
        mockAlmacen.setId(mockAlmacenId);

        mockCompraDetalle = new CompraDetalle();
        mockCompraDetalle.setId(mockCompraDetalleId);

        mockVentaDetalle = new VentaDetalle();
        mockVentaDetalle.setId(mockVentaDetalleId);

        mockKardex = new Kardex();
        mockKardex.setId(mockKardexId);
        mockKardex.setIdProducto(mockProducto);
        mockKardex.setIdAlmacen(mockAlmacen);
        mockKardex.setTipoMovimiento("ENTRADA_COMPRA");
        mockKardex.setCantidad(BigDecimal.TEN);
        mockKardex.setPrecio(BigDecimal.ONE);
        mockKardex.setFecha(OffsetDateTime.now());

        // 3. Reflexión
        registroField = DefaultFrm.class.getDeclaredField("registro");
        registroField.setAccessible(true);
        estadoField = DefaultFrm.class.getDeclaredField("estado");
        estadoField.setAccessible(true);

        productosDisponiblesField = KardexFrm.class.getDeclaredField("productosDisponibles");
        productosDisponiblesField.setAccessible(true);
        almacenesDisponiblesField = KardexFrm.class.getDeclaredField("almacenesDisponibles");
        almacenesDisponiblesField.setAccessible(true);
    }

    @AfterEach
    void tearDown() {
        if (mockedFacesContext != null) mockedFacesContext.close();
        if (mockedLogger != null) mockedLogger.close();
    }

    // ==========================================================================
    // 1. Inicialización y Carga de Filtros
    // ==========================================================================

    @Test
    void testInicializar_Success() {
        when(productoDAO.findAll()).thenReturn(List.of(mockProducto));
        when(almacenDAO.findAll()).thenReturn(List.of(mockAlmacen));
        when(compraDetalleDAO.findAll()).thenReturn(List.of(mockCompraDetalle));
        when(ventaDetalleDAO.findAll()).thenReturn(List.of(mockVentaDetalle));

        cut.inicializar();

        assertFalse(cut.getProductosDisponibles().isEmpty());
        assertFalse(cut.getAlmacenesDisponibles().isEmpty());
        assertFalse(cut.getComprasDetalleDisponibles().isEmpty());
        assertFalse(cut.getVentasDetalleDisponibles().isEmpty());

        verify(productoDAO).findAll();
    }

    @Test
    void testInicializar_Exception() {
        when(productoDAO.findAll()).thenThrow(new RuntimeException("DB Error"));

        cut.inicializar();

        assertTrue(cut.getProductosDisponibles().isEmpty());
        assertTrue(cut.getAlmacenesDisponibles().isEmpty());
    }

    @Test
    void testGettersListas_LazyLoad() throws Exception {
        productosDisponiblesField.set(cut, null);
        almacenesDisponiblesField.set(cut, null);

        when(productoDAO.findAll()).thenReturn(List.of(mockProducto));
        when(almacenDAO.findAll()).thenReturn(List.of(mockAlmacen));

        cut.getProductosDisponibles();
        cut.getAlmacenesDisponibles();
        cut.getComprasDetalleDisponibles();
        cut.getVentasDetalleDisponibles();

        verify(productoDAO, times(1)).findAll();
    }

    @Test
    void testGetTiposMovimiento() {
        List<String> tipos = cut.getTiposMovimiento();
        assertNotNull(tipos);
        assertTrue(tipos.contains("ENTRADA_COMPRA"));
    }

    // ==========================================================================
    // 2. Métodos Abstractos / CRUD Básico
    // ==========================================================================

    @Test
    void testGetFacesContext() {
        assertEquals(facesContext, cut.getFacesContext());
    }

    @Test
    void testGetDao() {
        assertEquals(kardexDAO, cut.getDao());
    }

    @Test
    void testNuevoRegistro() {
        Kardex nuevo = cut.nuevoRegistro();
        assertNotNull(nuevo);
        assertNotNull(nuevo.getId());
        assertEquals(BigDecimal.ZERO, nuevo.getCantidad());
        assertNotNull(nuevo.getIdProducto());
    }

    // ==========================================================================
    // 3. Búsqueda y Conversión
    // ==========================================================================

    @Test
    void testBuscarRegistroPorId_Exito() {
        when(kardexDAO.findById(mockKardexId)).thenReturn(mockKardex);
        assertEquals(mockKardex, cut.buscarRegistroPorId(mockKardexId));
    }

    @Test
    void testBuscarRegistroPorId_Fallos() {
        assertNull(cut.buscarRegistroPorId(null));
        assertNull(cut.buscarRegistroPorId("no-uuid"));
    }

    @Test
    void testGetIdAsText() {
        assertEquals(mockKardexId.toString(), cut.getIdAsText(mockKardex));
        assertNull(cut.getIdAsText(null));
        assertNull(cut.getIdAsText(new Kardex()));
    }

    @Test
    void testGetIdByText_Exito() {
        when(kardexDAO.findById(mockKardexId)).thenReturn(mockKardex);
        assertEquals(mockKardex, cut.getIdByText(mockKardexId.toString()));
    }

    @Test
    void testGetIdByText_Fallos() {
        assertNull(cut.getIdByText(null));
        assertNull(cut.getIdByText(""));
        assertNull(cut.getIdByText("invalid-uuid"));
    }

    // ==========================================================================
    // 4. Validaciones (esNombreVacio)
    // ==========================================================================

    @Test
    void testEsNombreVacio_Success() {
        assertFalse(cut.esNombreVacio(mockKardex));
    }

    @Test
    void testEsNombreVacio_Fails() {
        // Producto
        mockKardex.setIdProducto(null);
        assertTrue(cut.esNombreVacio(mockKardex));
        mockKardex.setIdProducto(new Producto());
        assertTrue(cut.esNombreVacio(mockKardex));
        mockKardex.setIdProducto(mockProducto);

        // Almacen
        mockKardex.setIdAlmacen(null);
        assertTrue(cut.esNombreVacio(mockKardex));
        mockKardex.setIdAlmacen(new Almacen());
        assertTrue(cut.esNombreVacio(mockKardex));
        mockKardex.setIdAlmacen(mockAlmacen);

        // Tipo
        mockKardex.setTipoMovimiento(null);
        assertTrue(cut.esNombreVacio(mockKardex));
        mockKardex.setTipoMovimiento("  ");
        assertTrue(cut.esNombreVacio(mockKardex));
        mockKardex.setTipoMovimiento("ENTRADA");

        // Cantidad
        mockKardex.setCantidad(null);
        assertTrue(cut.esNombreVacio(mockKardex));
        mockKardex.setCantidad(BigDecimal.ZERO);
        assertTrue(cut.esNombreVacio(mockKardex));
        mockKardex.setCantidad(BigDecimal.TEN);

        // Precio
        mockKardex.setPrecio(null);
        assertTrue(cut.esNombreVacio(mockKardex));
        mockKardex.setPrecio(new BigDecimal("-1"));
        assertTrue(cut.esNombreVacio(mockKardex));
        mockKardex.setPrecio(BigDecimal.ONE);

        // Fecha
        mockKardex.setFecha(null);
        assertTrue(cut.esNombreVacio(mockKardex));

        verify(facesContext, atLeastOnce()).addMessage(isNull(), any());
    }

    // ==========================================================================
    // 5. Pruebas de Métodos Privados (Obtener Entidades)
    // ==========================================================================

    @Test
    void testObtenerProductoCompleto() throws Exception {
        Method method = KardexFrm.class.getDeclaredMethod("obtenerProductoCompleto", UUID.class);
        method.setAccessible(true);

        when(productoDAO.findById(mockProductoId)).thenReturn(mockProducto);
        assertEquals(mockProducto, method.invoke(cut, mockProductoId));
        assertNull(method.invoke(cut, (UUID) null));

        when(productoDAO.findById(mockProductoId)).thenThrow(new RuntimeException("DB Error"));
        assertNull(method.invoke(cut, mockProductoId));
    }

    @Test
    void testObtenerAlmacenCompleto() throws Exception {
        Method method = KardexFrm.class.getDeclaredMethod("obtenerAlmacenCompleto", Integer.class);
        method.setAccessible(true);

        when(almacenDAO.findById(mockAlmacenId)).thenReturn(mockAlmacen);
        assertEquals(mockAlmacen, method.invoke(cut, mockAlmacenId));
        assertNull(method.invoke(cut, (Integer) null));

        when(almacenDAO.findById(mockAlmacenId)).thenThrow(new RuntimeException("DB Error"));
        assertNull(method.invoke(cut, mockAlmacenId));
    }

    @Test
    void testObtenerCompraDetalleCompleto() throws Exception {
        Method method = KardexFrm.class.getDeclaredMethod("obtenerCompraDetalleCompleto", UUID.class);
        method.setAccessible(true);

        when(compraDetalleDAO.findById(mockCompraDetalleId)).thenReturn(mockCompraDetalle);
        assertEquals(mockCompraDetalle, method.invoke(cut, mockCompraDetalleId));
        assertNull(method.invoke(cut, (UUID) null));

        when(compraDetalleDAO.findById(mockCompraDetalleId)).thenThrow(new RuntimeException("Error"));
        assertNull(method.invoke(cut, mockCompraDetalleId));
    }

    @Test
    void testObtenerVentaDetalleCompleto() throws Exception {
        Method method = KardexFrm.class.getDeclaredMethod("obtenerVentaDetalleCompleto", UUID.class);
        method.setAccessible(true);

        when(ventaDetalleDAO.findById(mockVentaDetalleId)).thenReturn(mockVentaDetalle);
        assertEquals(mockVentaDetalle, method.invoke(cut, mockVentaDetalleId));
        assertNull(method.invoke(cut, (UUID) null));

        when(ventaDetalleDAO.findById(mockVentaDetalleId)).thenThrow(new RuntimeException("Error"));
        assertNull(method.invoke(cut, mockVentaDetalleId));
    }

    // ==========================================================================
    // 6. Pruebas de Cálculo de Inventario
    // ==========================================================================

    private void invokeCalcular(Kardex k) throws Exception {
        Method method = KardexFrm.class.getDeclaredMethod("calcularValoresActuales", Kardex.class);
        method.setAccessible(true);
        try {
            method.invoke(cut, k);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    @Test
    void testCalcular_Entrada_SinHistorial() throws Exception {
        mockKardex.setTipoMovimiento("ENTRADA_COMPRA");
        mockKardex.setCantidad(new BigDecimal("10"));
        mockKardex.setPrecio(new BigDecimal("5.00"));

        when(kardexDAO.findUltimoMovimiento(any(), any())).thenReturn(null);

        invokeCalcular(mockKardex);

        assertEquals(new BigDecimal("10"), mockKardex.getCantidadActual());
        assertEquals(new BigDecimal("5.00"), mockKardex.getPrecioActual());
    }

    @Test
    void testCalcular_Entrada_PromedioPonderado() throws Exception {
        Kardex ultimo = new Kardex();
        ultimo.setCantidadActual(new BigDecimal("10"));
        ultimo.setPrecioActual(new BigDecimal("10.00"));
        when(kardexDAO.findUltimoMovimiento(any(), any())).thenReturn(ultimo);

        mockKardex.setTipoMovimiento("ENTRADA_COMPRA");
        mockKardex.setCantidad(new BigDecimal("10"));
        mockKardex.setPrecio(new BigDecimal("20.00"));

        invokeCalcular(mockKardex);

        assertEquals(new BigDecimal("20"), mockKardex.getCantidadActual());
        assertEquals(new BigDecimal("15.00"), mockKardex.getPrecioActual());
    }

    @Test
    void testCalcular_Salida_Exito() throws Exception {
        Kardex ultimo = new Kardex();
        ultimo.setCantidadActual(new BigDecimal("20"));
        ultimo.setPrecioActual(new BigDecimal("15.00"));
        when(kardexDAO.findUltimoMovimiento(any(), any())).thenReturn(ultimo);

        mockKardex.setTipoMovimiento("SALIDA_VENTA");
        mockKardex.setCantidad(new BigDecimal("5"));

        invokeCalcular(mockKardex);

        assertEquals(new BigDecimal("15"), mockKardex.getCantidadActual());
        assertEquals(new BigDecimal("15.00"), mockKardex.getPrecioActual());
    }

    @Test
    void testCalcular_Salida_StockInsuficiente() {
        Kardex ultimo = new Kardex();
        ultimo.setCantidadActual(new BigDecimal("5"));
        ultimo.setPrecioActual(new BigDecimal("10.00"));
        when(kardexDAO.findUltimoMovimiento(any(), any())).thenReturn(ultimo);

        mockKardex.setTipoMovimiento("SALIDA_VENTA");
        mockKardex.setCantidad(new BigDecimal("10"));

        Exception ex = assertThrows(Exception.class, () -> invokeCalcular(mockKardex));
        assertTrue(ex.getMessage().contains("No hay suficiente inventario"));
    }

    @Test
    void testCalcular_ExceptionGeneral() {
        when(kardexDAO.findUltimoMovimiento(any(), any())).thenThrow(new RuntimeException("DB Fail"));
        assertThrows(RuntimeException.class, () -> invokeCalcular(mockKardex));
    }

    // ==========================================================================
    // 7. Pruebas de Guardado (btnGuardarHandler)
    // ==========================================================================

    @Test
    void testBtnGuardarHandler_RegistroNull() throws IllegalAccessException {
        registroField.set(cut, null);
        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getDetail().contains("No hay registro")));
        verify(kardexDAO, never()).crear(any());
    }

    @Test
    void testBtnGuardarHandler_FallaValidacion() throws Exception {
        mockKardex.setIdProducto(null);
        registroField.set(cut, mockKardex);

        cut.btnGuardarHandler(actionEvent);

        verify(kardexDAO, never()).crear(any());
        verify(facesContext, atLeastOnce()).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN));
    }

    @Test
    void testBtnGuardarHandler_EntidadesNoEncontradas() throws Exception {
        registroField.set(cut, mockKardex);

        // Caso: Producto no encontrado
        when(productoDAO.findById(mockProductoId)).thenReturn(null);
        when(almacenDAO.findById(mockAlmacenId)).thenReturn(mockAlmacen);

        cut.btnGuardarHandler(actionEvent);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getDetail().contains("Producto")));
        verify(kardexDAO, never()).crear(any());

        // Reset y Caso: Almacén no encontrado
        reset(facesContext);
        when(productoDAO.findById(mockProductoId)).thenReturn(mockProducto);
        when(almacenDAO.findById(mockAlmacenId)).thenReturn(null);

        cut.btnGuardarHandler(actionEvent);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getDetail().contains("Almacén")));
    }

    @Test
    void testBtnGuardarHandler_Exito_ConOpcionales() throws Exception {
        mockKardex.setIdCompraDetalle(mockCompraDetalle);
        mockKardex.setIdVentaDetalle(mockVentaDetalle);
        registroField.set(cut, mockKardex);

        when(productoDAO.findById(mockProductoId)).thenReturn(mockProducto);
        when(almacenDAO.findById(mockAlmacenId)).thenReturn(mockAlmacen);
        when(compraDetalleDAO.findById(mockCompraDetalleId)).thenReturn(mockCompraDetalle);
        when(ventaDetalleDAO.findById(mockVentaDetalleId)).thenReturn(mockVentaDetalle);
        when(kardexDAO.findUltimoMovimiento(any(), any())).thenReturn(null);

        doNothing().when(cut).inicializarRegistros();

        cut.btnGuardarHandler(actionEvent);

        verify(kardexDAO).crear(mockKardex);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO));

        assertNull(registroField.get(cut));
    }

    @Test
    void testBtnGuardarHandler_Exito_SinOpcionales() throws Exception {
        mockKardex.setIdCompraDetalle(new CompraDetalle());
        mockKardex.setIdVentaDetalle(new VentaDetalle());
        registroField.set(cut, mockKardex);

        when(productoDAO.findById(mockProductoId)).thenReturn(mockProducto);
        when(almacenDAO.findById(mockAlmacenId)).thenReturn(mockAlmacen);
        when(kardexDAO.findUltimoMovimiento(any(), any())).thenReturn(null);

        cut.btnGuardarHandler(actionEvent);

        verify(kardexDAO).crear(mockKardex);
        assertNull(mockKardex.getIdCompraDetalle());
        assertNull(mockKardex.getIdVentaDetalle());
    }

    @Test
    void testBtnGuardarHandler_Exception() throws Exception {
        registroField.set(cut, mockKardex);
        when(productoDAO.findById(mockProductoId)).thenThrow(new RuntimeException("Error Crítico"));

        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
        verify(kardexDAO, never()).crear(any());
    }

    // ==========================================================================
    // 8. Pruebas de Restricciones
    // ==========================================================================

    @Test
    void testBtnModificarHandler() {
        cut.btnModificarHandler(actionEvent);
        verify(facesContext).addMessage(isNull(), argThat(m ->
                m.getSeverity() == FacesMessage.SEVERITY_WARN &&
                        m.getDetail().contains("No se permite modificar")));

        verify(kardexDAO, never()).modificar(any());
    }

    @Test
    void testBtnEliminarHandler() {
        cut.btnEliminarHandler(actionEvent);
        verify(facesContext).addMessage(isNull(), argThat(m ->
                m.getSeverity() == FacesMessage.SEVERITY_WARN &&
                        m.getDetail().contains("No se permite eliminar")));

        verify(kardexDAO, never()).eliminar(any());
    }
}