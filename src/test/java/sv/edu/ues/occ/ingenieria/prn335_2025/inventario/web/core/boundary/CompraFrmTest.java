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
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.Compra;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.Proveedor;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompraFrmTest {

    // --- Mocks ---
    @Mock CompraDetalleDAO compraDetalleDAO;
    @Mock CompraDAO compraDao;
    @Mock ProveedorDAO proveedorDao;
    @Mock NotificadorKardex notificadorKardex;
    @Mock FacesContext facesContext;
    @Mock ActionEvent actionEvent;

    // Configuración para silenciar el Logger
    private MockedStatic<FacesContext> mockedFacesContext;
    private MockedStatic<Logger> mockedLogger;
    private Logger appLogger;

    @Spy
    @InjectMocks
    CompraFrm cut;

    // --- Entidades y IDs ---
    private Compra mockCompra;
    private Long mockCompraId = 1L;
    private Integer mockProveedorId = 50;
    private Proveedor mockProveedor;

    // --- Reflexión para acceder a campos protegidos/privados ---
    private Field estadoField;
    private Field registroField;
    private Field proveedoresDisponiblesField;

    @BeforeEach
    void setUp() throws Exception {
        // Mock FacesContext
        mockedFacesContext = mockStatic(FacesContext.class);
        mockedFacesContext.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
        doNothing().when(facesContext).addMessage(any(), any(FacesMessage.class));

        // Configurar Logger para silenciar la salida
        appLogger = Logger.getLogger(CompraFrm.class.getName());
        appLogger.setLevel(Level.OFF);
        mockedLogger = mockStatic(Logger.class);
        mockedLogger.when(() -> Logger.getLogger(anyString())).thenReturn(appLogger);

        // Inicializar Entidades
        mockProveedor = new Proveedor();
        mockProveedor.setId(mockProveedorId);
        mockProveedor.setNombre("Proveedor Test");

        mockCompra = new Compra();
        mockCompra.setId(mockCompraId);
        mockCompra.setFecha(OffsetDateTime.now());
        mockCompra.setEstado(EstadoCompra.CREADA.name());
        mockCompra.setIdProveedor(mockProveedorId);
        mockCompra.setProveedor(mockProveedor);

        // 3. Reflexión
        estadoField = DefaultFrm.class.getDeclaredField("estado");
        estadoField.setAccessible(true);
        registroField = DefaultFrm.class.getDeclaredField("registro");
        registroField.setAccessible(true);
        proveedoresDisponiblesField = CompraFrm.class.getDeclaredField("proveedoresDisponibles");
        proveedoresDisponiblesField.setAccessible(true);
    }

    @AfterEach
    void tearDown() {
        if (mockedFacesContext != null) mockedFacesContext.close();
        if (mockedLogger != null) mockedLogger.close();
    }

    // ----------------------------------------------------------------------
    // --- 1. Métodos Heredados y Auxiliares ---
    // ----------------------------------------------------------------------

    @Test
    void testGetFacesContext() {
        assertEquals(facesContext, cut.getFacesContext());
    }

    @Test
    void testGetDao() {
        assertEquals(compraDao, cut.getDao());
    }

    @Test
    void testNuevoRegistro() {
        Compra k = cut.nuevoRegistro();
        assertNotNull(k);
        assertNotNull(k.getFecha());
    }

    @Test
    void testBuscarRegistroPorId_Success() throws Exception {
        when(compraDao.findById(mockCompraId)).thenReturn(mockCompra);
        assertEquals(mockCompra, cut.buscarRegistroPorId(mockCompraId));
    }

    @Test
    void testBuscarRegistroPorId_NotFoundOrInvalid() throws Exception {
        when(compraDao.findById(anyLong())).thenReturn(null);
        assertNull(cut.buscarRegistroPorId(999L));
        assertNull(cut.buscarRegistroPorId("invalid"));
    }

    @Test
    void testBuscarRegistroPorId_Exception() throws Exception {
        when(compraDao.findById(mockCompraId)).thenThrow(new RuntimeException("DB Error"));
        assertNull(cut.buscarRegistroPorId(mockCompraId));
    }

    @Test
    void testGetIdAsText() {
        assertEquals(mockCompraId.toString(), cut.getIdAsText(mockCompra));
        assertNull(cut.getIdAsText(null));
        Compra c = new Compra();
        assertNull(cut.getIdAsText(c));
    }

    @Test
    void testGetIdByText_Success() throws Exception {
        doReturn(mockCompra).when(cut).buscarRegistroPorId(mockCompraId);
        assertEquals(mockCompra, cut.getIdByText(mockCompraId.toString()));
    }

    @Test
    void testGetIdByText_Invalid() {
        assertNull(cut.getIdByText(null));
        assertNull(cut.getIdByText("invalid-long"));
    }

    @Test
    void testEsNombreVacio() {
        assertTrue(cut.esNombreVacio(null));
        assertTrue(cut.esNombreVacio(new Compra()));
        assertFalse(cut.esNombreVacio(mockCompra));
    }

    // ----------------------------------------------------------------------
    // --- 2. PostConstruct y Getters ---
    // ----------------------------------------------------------------------

    @Test
    void testInitAndGetters() {
        when(proveedorDao.findAll()).thenReturn(Arrays.asList(mockProveedor));

        cut.init();

        List<Proveedor> resultProveedores = cut.getProveedoresDisponibles();
        assertFalse(resultProveedores.isEmpty());

        List<EstadoCompra> resultEstados = cut.getEstadosDisponibles();
        assertEquals(EstadoCompra.values().length, resultEstados.size());

        verify(cut, times(1)).inicializar();
    }

    // ----------------------------------------------------------------------
    // --- 3. Validación y Creación (crearEntidad) ---
    // ----------------------------------------------------------------------

    @Test
    void testCrearEntidad_Success() throws Exception {
        when(proveedorDao.findById(mockProveedorId)).thenReturn(mockProveedor);

        cut.crearEntidad(mockCompra);

        verify(compraDao).crear(mockCompra);
    }

    @Test
    void testCrearEntidad_Validation_FechaNull() {
        Compra invalid = new Compra();
        invalid.setIdProveedor(mockProveedorId);
        invalid.setEstado(EstadoCompra.CREADA.name());

        Exception exception = assertThrows(Exception.class, () -> cut.crearEntidad(invalid));
        assertEquals("La fecha es obligatoria", exception.getMessage());
    }

    @Test
    void testCrearEntidad_Validation_ProveedorNull() {
        Compra invalid = new Compra();
        invalid.setFecha(OffsetDateTime.now());
        invalid.setEstado(EstadoCompra.CREADA.name());

        Exception exception = assertThrows(Exception.class, () -> cut.crearEntidad(invalid));
        assertEquals("Debe seleccionar un proveedor", exception.getMessage());
    }

    @Test
    void testCrearEntidad_Validation_EstadoBlank() {
        Compra invalid = new Compra();
        invalid.setFecha(OffsetDateTime.now());
        invalid.setIdProveedor(mockProveedorId);
        invalid.setEstado(" ");

        Exception exception = assertThrows(Exception.class, () -> cut.crearEntidad(invalid));
        assertEquals("Debe seleccionar un estado", exception.getMessage());
    }

    @Test
    void testCrearEntidad_Validation_ProveedorNotFound() {
        when(proveedorDao.findById(anyInt())).thenReturn(null);

        Exception exception = assertThrows(Exception.class, () -> cut.crearEntidad(mockCompra));
        assertEquals("El proveedor seleccionado no existe.", exception.getMessage());
    }

    // ----------------------------------------------------------------------
    // --- 4. Guardar Handler (btnGuardarHandler) ---
    // ----------------------------------------------------------------------

    @Test
    void testBtnGuardarHandler_ValidationFail_RegistroNull() {
        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN && m.getDetail().contains("No hay registro")));
        verify(notificadorKardex, never()).notificarCambio(anyString());
    }

    @Test
    void testBtnGuardarHandler_ValidationFail_ProveedorNull() throws Exception {
        registroField.set(cut, new Compra());
        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN && m.getDetail().contains("proveedor")));
    }

    @Test
    void testBtnGuardarHandler_ValidationFail_EstadoNull() throws Exception {
        Compra c = new Compra();
        c.setIdProveedor(mockProveedorId);
        c.setEstado(null);
        registroField.set(cut, c);
        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN && m.getDetail().contains("estado")));
    }

    @Test
    void testBtnGuardarHandler_ValidationFail_FechaNull() throws Exception {
        Compra c = new Compra();
        c.setIdProveedor(mockProveedorId);
        c.setEstado(EstadoCompra.CREADA.name());
        c.setFecha(null);
        registroField.set(cut, c);
        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN && m.getDetail().contains("fecha")));
    }

    // --- Sub-path CREAR ---
    @Test
    void testBtnGuardarHandler_Crear_Success() throws Exception {
        estadoField.set(cut, ESTADO_CRUD.CREAR);
        registroField.set(cut, mockCompra);

        doNothing().when(cut).crearEntidad(mockCompra);

        cut.btnGuardarHandler(actionEvent);

        verify(cut).crearEntidad(mockCompra);
        verify(notificadorKardex).notificarCambio("RELOAD_TABLE");
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO));

        // Verify cleanup
        assertNull(registroField.get(cut));
        assertEquals(ESTADO_CRUD.NADA, estadoField.get(cut));
    }

    // --- Sub-path MODIFICAR ---
    @Test
    void testBtnGuardarHandler_Modificar_Success() throws Exception {
        estadoField.set(cut, ESTADO_CRUD.MODIFICAR);
        registroField.set(cut, mockCompra);

        when(proveedorDao.findById(mockProveedorId)).thenReturn(mockProveedor);

        // 🔥 CORRECCIÓN CLAVE: Usar when().thenReturn() para métodos que no son void
        when(compraDao.modificar(mockCompra)).thenReturn(mockCompra);

        cut.btnGuardarHandler(actionEvent);

        verify(compraDao).validarProveedor(mockProveedorId);
        verify(proveedorDao).findById(mockProveedorId);
        verify(compraDao).modificar(mockCompra);
        verify(notificadorKardex).notificarCambio("RELOAD_TABLE");
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO));
    }

    @Test
    void testBtnGuardarHandler_Exception() throws Exception {
        estadoField.set(cut, ESTADO_CRUD.MODIFICAR);
        registroField.set(cut, mockCompra);

        when(proveedorDao.findById(mockProveedorId)).thenReturn(mockProveedor);

        // 🔥 CORRECCIÓN CLAVE: Usar when().thenThrow() para simular la excepción en un método no-void
        when(compraDao.modificar(mockCompra)).thenThrow(new RuntimeException("Error BD"));

        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR && m.getDetail().contains("Error BD")));
    }

    @Test
    void testBtnGuardarHandler_Exception_WithNestedCause() throws Exception {
        estadoField.set(cut, ESTADO_CRUD.CREAR);
        registroField.set(cut, mockCompra);

        Exception nestedException = new Exception("Nested constraint violation");
        doThrow(new RuntimeException("Parent error", nestedException)).when(cut).crearEntidad(mockCompra);

        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(),
                argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR && m.getDetail().contains("Nested constraint violation")));
    }


    // ----------------------------------------------------------------------
    // --- 5. Cerrar Compra Handler (btnCerrarCompraHandler) ---
    // ----------------------------------------------------------------------

    @Test
    void testBtnCerrarCompraHandler_ValidationFail_RegistroNull() {
        cut.btnCerrarCompraHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN && m.getDetail().contains("No hay registro seleccionado")));
        verify(compraDao, never()).modificar(any());
    }

    @Test
    void testBtnCerrarCompraHandler_Success() throws Exception {
        registroField.set(cut, mockCompra);

        // 🔥 CORRECCIÓN CLAVE: Usar when().thenReturn() para métodos que no son void
        when(compraDao.modificar(mockCompra)).thenReturn(mockCompra);

        cut.btnCerrarCompraHandler(actionEvent);

        assertEquals("PAGADA", mockCompra.getEstado());

        verify(compraDao).modificar(mockCompra);

        verify(notificadorKardex).notificarCambio("RELOAD_TABLE");

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO && m.getDetail().contains("cerrada correctamente")));
        assertNull(registroField.get(cut));
    }

    @Test
    void testBtnCerrarCompraHandler_Exception() throws Exception {
        registroField.set(cut, mockCompra);
        // 🔥 CORRECCIÓN CLAVE: Usar when().thenThrow()
        when(compraDao.modificar(mockCompra)).thenThrow(new RuntimeException("Cierre Fallido"));

        cut.btnCerrarCompraHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR && m.getDetail().contains("No se pudo cerrar la compra")));
    }

    // ----------------------------------------------------------------------
    // --- 6. Obtener Total Compra (getTotalCompra) ---
    // ----------------------------------------------------------------------

    @Test
    void testGetTotalCompra_Success() {
        BigDecimal expectedTotal = new BigDecimal("150.75");
        when(compraDetalleDAO.obtenerTotalCompra(mockCompraId)).thenReturn(expectedTotal);

        BigDecimal result = cut.getTotalCompra(mockCompra);

        assertEquals(expectedTotal, result);
        verify(compraDetalleDAO).obtenerTotalCompra(mockCompraId);
    }

    @Test
    void testGetTotalCompra_NullOrNoId() {
        assertEquals(BigDecimal.ZERO, cut.getTotalCompra(null));

        assertEquals(BigDecimal.ZERO, cut.getTotalCompra(new Compra()));

        verify(compraDetalleDAO, never()).obtenerTotalCompra(anyLong());
    }
}