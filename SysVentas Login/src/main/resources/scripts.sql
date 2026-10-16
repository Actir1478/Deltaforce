MERGE INTO usuario (id_usuario, usuario, clave, estado, id_perfil) KEY (id_usuario)
    VALUES (2, 'dehaka', 'nelson123', 'ACTIVO', 1),
    (3, 'pepe', '123456', 'ACTIVO', 3);