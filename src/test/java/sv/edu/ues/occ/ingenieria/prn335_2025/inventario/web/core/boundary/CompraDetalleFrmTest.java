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
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.CompraDAO;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.CompraDetalleDAO;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.control.ProductoDAO;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.Compra;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.CompraDetalle;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.Producto;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompraDetalleFrmTest {

    @Mock CompraDetalleDAO compraDetalleDao;
    @Mock CompraDAO compraDao;
    @Mock ProductoDAO productoDao;
    @Mock FacesContext facesContext;
    @Mock ActionEvent actionEvent;

    private MockedStatic<FacesContext> mockedFacesContext;
    private MockedStatic<Logger> mockedLogger;

    @Spy
    @InjectMocks
    CompraDetalleFrm cut;

    private Compra mockCompra;
    private Producto mockProducto;
    private CompraDetalle mockDetalle;
    private CompraDetalle mockDetalleConFK;

    private UUID mockDetalleId = UUID.randomUUID();
    private Long mockCompraId = 10L;
    private UUID mockProductoId = UUID.randomUUID();

    private Field registroField;
    private Field estadoField;

    @BeforeEach
    void setUp() throws Exception {
        // Mock FacesContext
        mockedFacesContext = mockStatic(FacesContext.class);
        mockedFacesContext.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
        doNothing().when(facesContext).addMessage(any(), any(FacesMessage.class));

        // Mock Logger
        mockedLogger = mockStatic(Logger.class);
        mockedLogger.when(() -> Logger.getLogger(anyString())).thenReturn(mock(Logger.class));

        // Entidades
        mockCompra = new Compra();
        mockCompra.setId(mockCompraId);

        mockProducto = new Producto();
        mockProducto.setId(mockProductoId);

        mockDetalle = cut.nuevoRegistro();
        mockDetalle.setId(mockDetalleId);
        mockDetalle.setCantidad(new BigDecimal("10"));
        mockDetalle.setPrecio(new BigDecimal("5.00"));
        mockDetalle.setEstado("RECIBIDO");
        mockDetalle.setIdCompra(mockCompra);
        mockDetalle.setIdProducto(mockProducto);

        mockDetalleConFK = new CompraDetalle();
        mockDetalleConFK.setId(mockDetalleId);
        mockDetalleConFK.setCantidad(new BigDecimal("10"));
        mockDetalleConFK.setPrecio(new BigDecimal("5.00"));
        mockDetalleConFK.setEstado("RECIBIDO");

        Compra cFK = new Compra(); cFK.setId(mockCompraId);
        Producto pFK = new Producto(); pFK.setId(mockProductoId);
        mockDetalleConFK.setIdCompra(cFK);
        mockDetalleConFK.setIdProducto(pFK);

        // Reflexión
        registroField = DefaultFrm.class.getDeclaredField("registro");
        registroField.setAccessible(true);
        estadoField = DefaultFrm.class.getDeclaredField("estado");
        estadoField.setAccessible(true);
    }

    @AfterEach
    void tearDown() {
        if (mockedFacesContext != null) mockedFacesContext.close();
        if (mockedLogger != null) mockedLogger.close();
    }

    // ----------------------------------------------------------------------
    // 1. Inicialización
    // ----------------------------------------------------------------------

    @Test
    void testInicializar_Success() {
        when(compraDao.findAll()).thenReturn(List.of(mockCompra));
        when(productoDao.findAll()).thenReturn(List.of(mockProducto));

        cut.inicializar();

        assertFalse(cut.getComprasDisponibles().isEmpty());
        assertFalse(cut.getProductosDisponibles().isEmpty());

        verify(compraDao).findAll();
        verify(productoDao).findAll();
    }

    @Test
    void testInicializar_ErrorEnCarga() {
        when(compraDao.findAll()).thenThrow(new RuntimeException("DB Error"));

        cut.inicializar();

        assertTrue(cut.getComprasDisponibles().isEmpty());
        assertTrue(cut.getProductosDisponibles().isEmpty());

        verify(compraDao).findAll();
        verify(productoDao, never()).findAll();
    }

    @Test
    void testGetters() {
        cut.getComprasDisponibles();
        cut.getProductosDisponibles();
        verify(compraDao, times(1)).findAll();
        verify(productoDao, times(1)).findAll();
    }

    // ----------------------------------------------------------------------
    // 2. Métodos Abstractos
    // ----------------------------------------------------------------------

    @Test
    void testGetDao() {
        assertEquals(compraDetalleDao, cut.getDao());
    }

    @Test
    void testNuevoRegistro() {
        CompraDetalle cd = cut.nuevoRegistro();
        assertNotNull(cd);
        assertNotNull(cd.getId());
        assertEquals(BigDecimal.ZERO, cd.getCantidad());
    }

    @Test
    void testBuscarRegistroPorId() {
        when(compraDetalleDao.findById(mockDetalleId)).thenReturn(mockDetalle);
        assertEquals(mockDetalle, cut.buscarRegistroPorId(mockDetalleId));
        assertNull(cut.buscarRegistroPorId("not a UUID"));
    }

    @Test
    void testGetIdAsText() {
        assertEquals(mockDetalleId.toString(), cut.getIdAsText(mockDetalle));
        assertNull(cut.getIdAsText(new CompraDetalle()));
    }

    @Test
    void testGetIdByText() {
        when(compraDetalleDao.findById(mockDetalleId)).thenReturn(mockDetalle);
        assertEquals(mockDetalle, cut.getIdByText(mockDetalleId.toString()));
        assertNull(cut.getIdByText(null));
        assertNull(cut.getIdByText("invalid-uuid"));
    }

    @Test
    void testSetGetIdCompra() {
        cut.setIdCompra(100L);
        assertEquals(100L, cut.getIdCompra());
    }

    // ----------------------------------------------------------------------
    // 3. Validación
    // ----------------------------------------------------------------------

    @Test
    void testEsNombreVacio_Success() {
        assertFalse(cut.esNombreVacio(mockDetalle));
    }

    @Test
    void testEsNombreVacio_Fallos() {
        CompraDetalle invalid = new CompraDetalle();
        invalid.setIdProducto(mockProducto);
        invalid.setCantidad(BigDecimal.ONE);
        invalid.setPrecio(BigDecimal.ONE);
        invalid.setEstado("OK");

        // Compra null
        assertTrue(cut.esNombreVacio(invalid));

        // Producto null
        invalid.setIdCompra(mockCompra);
        invalid.setIdProducto(null);
        assertTrue(cut.esNombreVacio(invalid));

        // Cantidad
        invalid.setIdProducto(mockProducto);
        invalid.setCantidad(BigDecimal.ZERO);
        assertTrue(cut.esNombreVacio(invalid));

        // Precio
        invalid.setCantidad(BigDecimal.TEN);
        invalid.setPrecio(null);
        assertTrue(cut.esNombreVacio(invalid));

        // Estado
        invalid.setPrecio(BigDecimal.TEN);
        invalid.setEstado(null);
        assertTrue(cut.esNombreVacio(invalid));

        verify(facesContext, atLeastOnce()).addMessage(any(), any());
    }

    // ----------------------------------------------------------------------
    // 4. Guardar Handler
    // ----------------------------------------------------------------------

    @Test
    void testBtnGuardarHandler_RegistroNull() {
        cut.btnGuardarHandler(actionEvent);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN));
        verify(compraDetalleDao, never()).crear(any());
    }

    @Test
    void testBtnGuardarHandler_ValidationFail() throws Exception {
        registroField.set(cut, mockDetalleConFK);
        mockDetalleConFK.setCantidad(BigDecimal.ZERO);

        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN));
        verify(compraDetalleDao, never()).crear(any());
    }

    @Test
    void testBtnGuardarHandler_FKNotFound() throws Exception {
        registroField.set(cut, mockDetalleConFK);

        when(compraDao.findById(mockCompraId)).thenReturn(null);
        when(productoDao.findById(mockProductoId)).thenReturn(mockProducto);

        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
        verify(compraDetalleDao, never()).crear(any());
    }

    @Test
    void testBtnGuardarHandler_Success() throws Exception {
        registroField.set(cut, mockDetalleConFK);

        when(compraDao.findById(mockCompraId)).thenReturn(mockCompra);
        when(productoDao.findById(mockProductoId)).thenReturn(mockProducto);

        cut.btnGuardarHandler(actionEvent);

        verify(compraDetalleDao).crear(mockDetalleConFK);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO));
    }

    @Test
    void testBtnGuardarHandler_PersistenceError() throws Exception {
        registroField.set(cut, mockDetalleConFK);

        when(compraDao.findById(mockCompraId)).thenReturn(mockCompra);
        when(productoDao.findById(mockProductoId)).thenReturn(mockProducto);

        doThrow(new RuntimeException("Error de BD")).when(compraDetalleDao).crear(any());

        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
    }

    // ----------------------------------------------------------------------
    // 5. Modificar Handler
    // ----------------------------------------------------------------------

    @Test
    void testBtnModificarHandler_RegistroNull() {
        cut.btnModificarHandler(actionEvent);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN));
    }

    @Test
    void testBtnModificarHandler_Success() throws Exception {
        registroField.set(cut, mockDetalleConFK);

        when(compraDao.findById(mockCompraId)).thenReturn(mockCompra);
        when(productoDao.findById(mockProductoId)).thenReturn(mockProducto);
        when(compraDetalleDao.modificar(mockDetalleConFK)).thenReturn(mockDetalleConFK);

        cut.btnModificarHandler(actionEvent);

        verify(compraDetalleDao).modificar(mockDetalleConFK);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO));
    }

    @Test
    void testBtnModificarHandler_PersistenceError() throws Exception {
        registroField.set(cut, mockDetalleConFK);

        when(compraDao.findById(mockCompraId)).thenReturn(mockCompra);
        when(productoDao.findById(mockProductoId)).thenReturn(mockProducto);
        when(compraDetalleDao.modificar(mockDetalleConFK)).thenThrow(new RuntimeException("Error"));

        cut.btnModificarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
    }

    // ----------------------------------------------------------------------
    // 6. Carga de Datos y LazyDataModel
    // ----------------------------------------------------------------------

    @Test
    void testCargarDatos_Success() {
        cut.setIdCompra(mockCompraId);
        when(compraDetalleDao.findByIdCompra(mockCompraId)).thenReturn(List.of(mockDetalle));

        List<CompraDetalle> result = cut.cargarDatos(0, 10);
        assertFalse(result.isEmpty());
    }

    @Test
    void testCargarDatos_Error() {
        cut.setIdCompra(mockCompraId);
        when(compraDetalleDao.findByIdCompra(mockCompraId)).thenThrow(new RuntimeException("Err"));
        assertTrue(cut.cargarDatos(0, 10).isEmpty());
    }

    /**
     * CORRECCIÓN CRÍTICA: Este test ahora verifica el flujo correcto del LazyDataModel
     * donde el rowCount se actualiza después de llamar a count() o load().
     */
    @Test
    void testCargarDatosPorCompra_LazyModel() throws Exception {
        // 1. Setup del Mock de conteo
        when(compraDetalleDao.contarDetallesPorCompra(mockCompraId)).thenReturn(5L);

        // 2. Inicializar el LazyModel
        cut.cargarDetallesPorCompra(mockCompraId);

        // Obtener el modelo
        Field modeloField = DefaultFrm.class.getDeclaredField("modelo");
        modeloField.setAccessible(true);
        LazyDataModel<CompraDetalle> modelo = (LazyDataModel<CompraDetalle>) modeloField.get(cut);
        assertNotNull(modelo);

        // 3. Ejecutar count() explícitamente para verificar la implementación anónima
        int conteo = modelo.count(Collections.emptyMap());
        assertEquals(5, conteo, "El método count() del LazyDataModel debe retornar 5");

        // 4. Configurar el Mock de carga
        when(compraDetalleDao.findByIdCompra(mockCompraId)).thenReturn(List.of(mockDetalle));

        // 5. Ejecutar load().
        // IMPORTANTE: Tu código fuente llama a setRowCount() dentro de load().
        // Esto actualizará el getRowCount() del modelo.
        List<CompraDetalle> list = modelo.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        assertFalse(list.isEmpty());

        // 6. AHORA verificamos el getRowCount(), que debe ser 5
        assertEquals(5, modelo.getRowCount(), "El rowCount debe ser 5 después de ejecutar load()");

        // 7. Otros métodos del modelo
        when(compraDetalleDao.findById(mockDetalleId)).thenReturn(mockDetalle);
        assertEquals(mockDetalle, modelo.getRowData(mockDetalleId.toString()));
        assertEquals(mockDetalleId.toString(), modelo.getRowKey(mockDetalle));
    }
}