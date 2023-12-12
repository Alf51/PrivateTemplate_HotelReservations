package org.goldenalf.privatepr.services.impl;

import org.goldenalf.privatepr.models.Client;
import org.goldenalf.privatepr.models.Role;
import org.goldenalf.privatepr.repositories.ClientRepository;
import org.goldenalf.privatepr.utils.VerifyingAccess;
import org.goldenalf.privatepr.utils.erorsHandler.ErrorHandler;
import org.goldenalf.privatepr.utils.exeptions.ClientErrorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VerifyingAccess verifyingAccess;
    @Mock
    private ErrorHandler errorHandler;
    @InjectMocks
    private ClientServiceImpl clientService;


    @Test
    void save_SaveClientFromClientService() {
        Client client = getClient();

        // Создаем ArgumentCaptor для захвата аргументов, переданных в passwordEncoder.encode
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        clientService.save(client);

        // Проверяем, что passwordEncoder.encode был вызван с правильным паролем
        verify(passwordEncoder).encode(passwordCaptor.capture());

        // Проверяем, что у клиента установлены правильные роли
        assertEquals(Collections.singleton(Role.USER), client.getRoles());

        // Проверяем, что clientRepository.save был вызван с правильным клиентом
        verify(clientRepository).save(client);
    }

    @Test
    void delete_forExistingClient_deleteSuccess() {
        Client client = getClient();
        int clientId = client.getId();
        when(clientService.getClient(clientId)).thenReturn(Optional.of(client));

        clientService.delete(clientId);

        // Проверяем, что verifyingAccess.checkPossibilityAction был вызван с правильным логином
        verify(verifyingAccess).checkPossibilityAction(client.getLogin());

        // Проверяем, что clientRepository.deleteById был вызван с правильным id
        verify(clientRepository).deleteById(clientId);
    }

    @Test
    void delete_forNotExistingClient_deleteFailed() {
        int clientId = 22;
        when(clientService.getClient(clientId)).thenReturn(Optional.empty());
        when(errorHandler.getErrorMessage(anyString())).thenReturn("Error message");


        // Проверяем, что будет выброшено исключение ClientErrorException
        ClientErrorException exception = assertThrows(ClientErrorException.class, () -> {
            clientService.delete(clientId);
        });

        // Проверяем, что исключение содержит правильное сообщение
        assertEquals("Error message", exception.getMessage());

        // Проверяем, что verifyingAccess.checkPossibilityAction и clientRepository.deleteById НЕ были вызваны
        verify(verifyingAccess, never()).checkPossibilityAction(anyString());
        verify(clientRepository, never()).deleteById(anyInt());
    }

    @Test
    void update_forExistingClient_updateSuccess() {
        // Создаем существующего в БД клиента
        Client existingClient = getClient();
        int clientId = existingClient.getId();

        //Создаём клиента с новыми данными для обновления
        Client updatedClient = new Client();
        updatedClient.setId(clientId);
        updatedClient.setLogin("newLogin");
        updatedClient.setPassword("newPassword");
        String newPassword = updatedClient.getPassword();

        when(clientService.getClient(clientId)).thenReturn(Optional.of(existingClient));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");

        clientService.update(clientId, updatedClient);

        // Проверяем, что verifyingAccess.checkPossibilityAction был вызван с правильными логинами
        verify(verifyingAccess).checkPossibilityAction(updatedClient.getLogin(), existingClient.getLogin());

        // Проверяем, что passwordEncoder.encode был вызван с правильным паролем
        verify(passwordEncoder).encode(newPassword);

        // Проверяем, что clientRepository.save был вызван с правильным клиентом
        verify(clientRepository).save(updatedClient);
    }

    @Test
    void update_forNotExistingClient_updateFailed() {
        Client client = getClient();
        int clientId = client.getId();
        when(clientService.getClient(clientId)).thenReturn(Optional.empty());

        // Mocking errorHandler.getErrorMessage
        when(errorHandler.getErrorMessage(anyString())).thenReturn("Error message");

        // Проверяем, что будет выброшено исключение ClientErrorException
        ClientErrorException exception = assertThrows(ClientErrorException.class, () -> {
            clientService.update(clientId, client);
        });

        // Проверяем, что исключение содержит правильное сообщение
        assertEquals("Error message", exception.getMessage());

        // Проверяем, что verifyingAccess.checkPossibilityAction и clientRepository.save НЕ были вызваны
        verifyingAccess.checkPossibilityAction("dummy", "dummy");
        clientRepository.save(mock(Client.class));
    }


    private Client getClient() {
        Client client = new Client();
        client.setId(1);
        client.setName("Monarch");
        client.setLogin("Bee");
        client.setPassword("123456");

        return client;
    }
}