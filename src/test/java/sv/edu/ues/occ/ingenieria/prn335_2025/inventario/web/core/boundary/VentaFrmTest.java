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
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.Cliente;
import sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.entity.Venta;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
class VentaFrmTest {

    @Mock VentaDAO ventaDao;
    @Mock ClienteDAO clienteDao;
    @Mock VentaDetalleDAO ventaDetalleDAO;
    @Mock NotificadorKardex notificadorKardex;
    @Mock FacesContext facesContext;
    @Mock ActionEvent actionEvent;

    private MockedStatic<FacesContext> mockedFacesContext;

    @Spy
    @InjectMocks
    VentaFrm cut;

    private Venta mockVenta;
    private Cliente mockCliente;
    private UUID mockVentaId = UUID.randomUUID();
    private UUID mockClienteId = UUID.randomUUID();

    // Campos de reflexión
    private Field registroField;
    private Field estadoField;
    private Field modeloField;
    private Class<?> estadoCrudClass;
    private Object estadoCrudNada;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Mockear FacesContext
        mockedFacesContext = mockStatic(FacesContext.class);
        mockedFacesContext.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
        doNothing().when(facesContext).addMessage(any(), any(FacesMessage.class));

        // 2. Inicializar Entidades
        mockCliente = new Cliente();
        mockCliente.setId(mockClienteId);
        mockCliente.setNombre("Cliente Test");

        mockVenta = new Venta();
        mockVenta.setId(mockVentaId);
        mockVenta.setIdCliente(mockCliente);
        mockVenta.setEstado("CREADA");
        mockVenta.setFecha(OffsetDateTime.now());

        // 3. Reflexión para campos heredados
        registroField = cut.getClass().getSuperclass().getDeclaredField("registro");
        registroField.setAccessible(true);
        estadoField = cut.getClass().getSuperclass().getDeclaredField("estado");
        estadoField.setAccessible(true);
        modeloField = cut.getClass().getSuperclass().getDeclaredField("modelo");
        modeloField.setAccessible(true);

        // 4. Cargar Enum ESTADO_CRUD
        // Intentamos cargar asumiendo que está en el mismo paquete (independiente)
        // O si es anidado, ajusta el nombre aquí. El código asume paquete estándar.
        String enumClassName = "sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.boundary.ESTADO_CRUD";
        try {
            estadoCrudClass = Class.forName(enumClassName);
            estadoCrudNada = Enum.valueOf((Class<Enum>) estadoCrudClass, "NADA");
        } catch (ClassNotFoundException e) {
            // Si falla, intentamos como clase anidada por si acaso
            try {
                estadoCrudClass = Class.forName("sv.edu.ues.occ.ingenieria.prn335_2025.inventario.web.core.boundary.DefaultFrm$ESTADO_CRUD");
                estadoCrudNada = Enum.valueOf((Class<Enum>) estadoCrudClass, "NADA");
            } catch (Exception ex) {
                throw new RuntimeException("No se pudo cargar ESTADO_CRUD", ex);
            }
        }
    }

    @AfterEach
    void tearDown() {
        if (mockedFacesContext != null) mockedFacesContext.close();
    }

    // ----------------------------------------------------------------------
    // --- 1. Inicialización y Carga de Datos ---
    // ----------------------------------------------------------------------

    @Test
    void testInicializar_Success() {
        when(clienteDao.findAll()).thenReturn(List.of(mockCliente));
        cut.inicializar();

        assertNotNull(cut.getClientesDisponibles());
        assertFalse(cut.getClientesDisponibles().isEmpty());
    }

    @Test
    void testCargarClientes_Exception() throws Exception {
        // Forzar excepción en el DAO
        when(clienteDao.findAll()).thenThrow(new RuntimeException("DB Error"));

        // Forzar recarga
        Field clientesField = cut.getClass().getDeclaredField("clientesDisponibles");
        clientesField.setAccessible(true);
        clientesField.set(cut, null);

        cut.getClientesDisponibles();

        assertTrue(cut.getClientesDisponibles().isEmpty());
    }

    @Test
    void testGetClientesDisponibles_Cached() throws Exception {
        // Caso donde la lista YA existe (rama 'else' o 'if false' del lazy load)
        Field clientesField = cut.getClass().getDeclaredField("clientesDisponibles");
        clientesField.setAccessible(true);
        clientesField.set(cut, new ArrayList<>()); // Lista vacía pero no null

        List<Cliente> result = cut.getClientesDisponibles();
        assertNotNull(result);
        // No debería llamar al DAO si ya no es null/empty (dependiendo de tu lógica exacta,
        // tu lógica dice: if (clientesDisponibles == null || clientesDisponibles.isEmpty())
        // Si le paso una lista vacía, entra al if y llama al DAO.
        // Si le paso una lista con datos, NO llama al DAO.

        clientesField.set(cut, List.of(mockCliente)); // Lista con datos
        cut.getClientesDisponibles();

        // Verificar que findAll NO se llamó nuevamente para la lista poblada
        // (Nota: puede haberse llamado en el setup o test previo, así que verificamos el conteo o reset)
        verify(clienteDao, atMost(1)).findAll();
    }

    @Test
    void testGetEstadosDisponibles() {
        assertNotNull(cut.getEstadosDisponibles());
        assertTrue(cut.getEstadosDisponibles().contains("CREADA"));
    }

    // ----------------------------------------------------------------------
    // --- 2. Métodos Base y Abstractos ---
    // ----------------------------------------------------------------------

    @Test
    void testGetFacesContext() {
        assertEquals(facesContext, cut.getFacesContext());
    }

    @Test
    void testGetDao() {
        assertEquals(ventaDao, cut.getDao());
    }

    @Test
    void testNuevoRegistro() {
        Venta nueva = cut.nuevoRegistro();
        assertNotNull(nueva);
        assertNotNull(nueva.getId());
        assertEquals("CREADA", nueva.getEstado());
    }

    // ----------------------------------------------------------------------
    // --- 3. Búsqueda y Conversión ---
    // ----------------------------------------------------------------------

    @Test
    void testBuscarRegistroPorId_Success() {
        when(ventaDao.findById(mockVentaId)).thenReturn(mockVenta);
        // Object UUID
        assertEquals(mockVenta, cut.buscarRegistroPorId(mockVentaId));
        // Object String
        assertEquals(mockVenta, cut.buscarRegistroPorId(mockVentaId.toString()));
    }

    @Test
    void testBuscarRegistroPorId_Null() {
        assertNull(cut.buscarRegistroPorId(null));
    }

    @Test
    void testBuscarRegistroPorId_Exception() {
        // Simular error al convertir UUID o error en DAO para cubrir el catch
        when(ventaDao.findById(any())).thenThrow(new RuntimeException("Error DB"));
        assertNull(cut.buscarRegistroPorId(mockVentaId));
    }

    @Test
    void testGetIdAsText() {
        assertEquals(mockVentaId.toString(), cut.getIdAsText(mockVenta));
        assertNull(cut.getIdAsText(null));

        Venta vNullId = new Venta();
        assertNull(cut.getIdAsText(vNullId));
    }

    @Test
    void testGetIdByText_Success() {
        when(ventaDao.findById(mockVentaId)).thenReturn(mockVenta);
        assertEquals(mockVenta, cut.getIdByText(mockVentaId.toString()));
    }

    @Test
    void testGetIdByText_Invalid() {
        assertNull(cut.getIdByText(null));
        assertNull(cut.getIdByText(""));
        // Cubre el catch (IllegalArgumentException)
        assertNull(cut.getIdByText("invalid-uuid"));
    }

    // ----------------------------------------------------------------------
    // --- 4. Validaciones ---
    // ----------------------------------------------------------------------

    @Test
    void testEsNombreVacio() {
        // Null object
        assertTrue(cut.esNombreVacio(null));

        // Cliente null cases
        mockVenta.setIdCliente(null);
        assertTrue(cut.esNombreVacio(mockVenta));
        mockVenta.setIdCliente(new Cliente()); // id null
        assertTrue(cut.esNombreVacio(mockVenta));
        mockVenta.setIdCliente(mockCliente); // restore

        // Estado null/empty
        mockVenta.setEstado(null);
        assertTrue(cut.esNombreVacio(mockVenta));
        mockVenta.setEstado("");
        assertTrue(cut.esNombreVacio(mockVenta));
        mockVenta.setEstado("CREADA"); // restore

        // Fecha null
        mockVenta.setFecha(null);
        assertTrue(cut.esNombreVacio(mockVenta));
        mockVenta.setFecha(OffsetDateTime.now()); // restore

        // Success
        assertFalse(cut.esNombreVacio(mockVenta));
    }

    // ----------------------------------------------------------------------
    // --- 5. Handlers: btnGuardarHandler ---
    // ----------------------------------------------------------------------

    @Test
    void testBtnGuardarHandler_NullRegistro() {
        cut.btnGuardarHandler(actionEvent); // registro es null por defecto en spy si no se setea
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN));
    }

    @Test
    void testBtnGuardarHandler_ValidationFail() throws Exception {
        mockVenta.setIdCliente(null);
        registroField.set(cut, mockVenta);
        cut.btnGuardarHandler(actionEvent);
        verify(ventaDao, never()).crear(any());
    }

    @Test
    void testBtnGuardarHandler_ClienteNotFound() throws Exception {
        registroField.set(cut, mockVenta);
        when(clienteDao.findById(mockClienteId)).thenReturn(null);

        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
    }

    @Test
    void testBtnGuardarHandler_Success_NotificationThrows() throws Exception {
        // Este test cubre el TRY-CATCH interno de la notificación
        registroField.set(cut, mockVenta);
        when(clienteDao.findById(mockClienteId)).thenReturn(mockCliente);
        doNothing().when(cut).inicializarRegistros();

        // Simulamos que la notificación falla
        doThrow(new RuntimeException("JMS Fail")).when(notificadorKardex).notificarCambio(anyString());

        cut.btnGuardarHandler(actionEvent);

        // Debe haber guardado a pesar del error de notificación
        verify(ventaDao).crear(mockVenta);
        // Debe mostrar mensaje de éxito
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO));
    }

    @Test
    void testBtnGuardarHandler_DAOException() throws Exception {
        // Este test cubre el TRY-CATCH externo
        registroField.set(cut, mockVenta);
        when(clienteDao.findById(mockClienteId)).thenReturn(mockCliente);
        doThrow(new RuntimeException("DB Error")).when(ventaDao).crear(any());

        cut.btnGuardarHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
    }

    // ----------------------------------------------------------------------
    // --- 6. Handlers: btnModificarHandler ---
    // ----------------------------------------------------------------------



    @Test
    void testBtnModificarHandler_NotificationThrows() throws Exception {
        // Cubre el TRY-CATCH interno de notificación en modificar
        registroField.set(cut, mockVenta);
        doThrow(new RuntimeException("JMS Fail")).when(notificadorKardex).notificarCambio(anyString());

        cut.btnModificarHandler(actionEvent);

        // Debe proceder con la modificación
        verify(ventaDao).modificar(mockVenta);
    }

    // ----------------------------------------------------------------------
    // --- 7. Handlers: btnCerrarVentaHandler ---
    // ----------------------------------------------------------------------

    @Test
    void testBtnCerrarVentaHandler_NullRegistro() {
        cut.btnCerrarVentaHandler(actionEvent);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN));
    }

    @Test
    void testBtnCerrarVentaHandler_Success_NotificationThrows() throws Exception {
        // Cubre éxito en lógica principal pero fallo en notificación (try-catch interno)
        registroField.set(cut, mockVenta);
        when(clienteDao.findById(mockClienteId)).thenReturn(mockCliente);
        doNothing().when(cut).inicializarRegistros();
        doThrow(new RuntimeException("JMS Fail")).when(notificadorKardex).notificarCambio(anyString());

        cut.btnCerrarVentaHandler(actionEvent);

        assertEquals("FINALIZADA", mockVenta.getEstado());
        verify(ventaDao).modificar(mockVenta);
        // Mensaje de éxito aunque falle notificación
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO));
    }

    @Test
    void testBtnCerrarVentaHandler_Exception() throws Exception {
        // Cubre try-catch externo
        registroField.set(cut, mockVenta);
        doThrow(new RuntimeException("DB Error")).when(ventaDao).modificar(any());

        cut.btnCerrarVentaHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
    }

    // ----------------------------------------------------------------------
    // --- 8. Handlers: btnAnularVentaHandler ---
    // ----------------------------------------------------------------------

    @Test
    void testBtnAnularVentaHandler_NullRegistro() {
        cut.btnAnularVentaHandler(actionEvent);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN));
    }

    @Test
    void testBtnAnularVentaHandler_Success_NotificationThrows() throws Exception {
        // Cubre éxito en lógica principal pero fallo en notificación (try-catch interno)
        registroField.set(cut, mockVenta);
        when(clienteDao.findById(mockClienteId)).thenReturn(mockCliente);
        doNothing().when(cut).inicializarRegistros();
        doThrow(new RuntimeException("JMS Fail")).when(notificadorKardex).notificarCambio(anyString());

        cut.btnAnularVentaHandler(actionEvent);

        assertEquals("ANULADA", mockVenta.getEstado());
        verify(ventaDao).modificar(mockVenta);
        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_INFO));
    }

    @Test
    void testBtnAnularVentaHandler_Exception() throws Exception {
        registroField.set(cut, mockVenta);
        doThrow(new RuntimeException("DB Error")).when(ventaDao).modificar(any());

        cut.btnAnularVentaHandler(actionEvent);

        verify(facesContext).addMessage(isNull(), argThat(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR));
    }

    // ----------------------------------------------------------------------
    // --- 9. Métodos Auxiliares (Total) ---
    // ----------------------------------------------------------------------

    @Test
    void testGetTotalVenta() {
        BigDecimal total = new BigDecimal("99.99");
        when(ventaDetalleDAO.obtenerTotalVenta(mockVentaId)).thenReturn(total);

        assertEquals(total, cut.getTotalVenta(mockVenta));
        assertEquals(BigDecimal.ZERO, cut.getTotalVenta(null));
        assertEquals(BigDecimal.ZERO, cut.getTotalVenta(new Venta())); // ID Null
    }
}