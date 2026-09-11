CREATE TABLE Usuario (
    idUsuario INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    autenticacao2FA BOOLEAN NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    CHECK (LENGTH(cpf) = 11)
);

CREATE TABLE Carteira (
    idCarteira INT PRIMARY KEY AUTO_INCREMENT,
    idUsuario INT NOT NULL,
    saldo DOUBLE NOT NULL CHECK (saldo >= 0),
    FOREIGN KEY (idUsuario) REFERENCES Usuario(idUsuario)
);

CREATE TABLE Ativos (
    idAtivo VARCHAR(255) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    valorAtual DOUBLE NOT NULL CHECK (valorAtual >= 0)
);

CREATE TABLE CriptoAtivo (
    idAtivo VARCHAR(255) PRIMARY KEY,
    variacaoDiaria DOUBLE NOT NULL,
    descricao TEXT,
    dataCriacao DATE,
    FOREIGN KEY (idAtivo) REFERENCES Ativos(idAtivo)
);

CREATE TABLE Transacao (
    idTransacao INT PRIMARY KEY AUTO_INCREMENT,
    idUsuario INT NOT NULL,
    idCarteira INT NOT NULL,
    idAtivo VARCHAR(255) NOT NULL,
    quantidade DOUBLE NOT NULL CHECK (quantidade > 0),
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('compra', 'venda')),
    data DATE NOT NULL,
    FOREIGN KEY (idUsuario) REFERENCES Usuario(idUsuario),
    FOREIGN KEY (idCarteira) REFERENCES Carteira(idCarteira),
    FOREIGN KEY (idAtivo) REFERENCES Ativos(idAtivo)
);

CREATE TABLE Operacao (
    idOperacao INT PRIMARY KEY AUTO_INCREMENT,
    idUsuario INT NOT NULL,
    quantidade DOUBLE NOT NULL CHECK (quantidade > 0),
    descricao TEXT,
    data DATE NOT NULL,
    FOREIGN KEY (idUsuario) REFERENCES Usuario(idUsuario)
);