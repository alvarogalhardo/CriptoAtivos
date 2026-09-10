-- Apagar tabelas caso existam
DROP TABLE Transacao CASCADE CONSTRAINTS PURGE;
DROP TABLE Operacao CASCADE CONSTRAINTS PURGE;
DROP TABLE CriptoAtivo CASCADE CONSTRAINTS PURGE;
DROP TABLE Ativos CASCADE CONSTRAINTS PURGE;
DROP TABLE Carteira CASCADE CONSTRAINTS PURGE;
DROP TABLE Usuario CASCADE CONSTRAINTS PURGE;

-- Criar tabela Usuario
CREATE TABLE Usuario (
    idUsuario NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR2(255) NOT NULL,
    email VARCHAR2(255) UNIQUE NOT NULL,
    senha VARCHAR2(255) NOT NULL,
    autenticacao2FA NUMBER(1) NOT NULL CHECK (autenticacao2FA IN (0,1)),
    cpf VARCHAR2(11) UNIQUE NOT NULL
);

-- Criar tabela Carteira
CREATE TABLE Carteira (
    idCarteira NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idUsuario NUMBER NOT NULL,
    saldo NUMBER(10,2) NOT NULL CHECK (saldo >= 0),
    FOREIGN KEY (idUsuario) REFERENCES Usuario(idUsuario) ON DELETE CASCADE
);

-- Criar tabela Ativos
CREATE TABLE Ativos (
    idAtivo VARCHAR2(255) PRIMARY KEY,
    nome VARCHAR2(255) NOT NULL UNIQUE,
    valorAtual NUMBER(10,2) NOT NULL CHECK (valorAtual >= 0)
);

-- Criar tabela CriptoAtivo
CREATE TABLE CriptoAtivo (
    idAtivo VARCHAR2(255) PRIMARY KEY,
    variacaoDiaria NUMBER(5,2) NOT NULL,
    descricao CLOB,
    dataCriacao DATE,
    FOREIGN KEY (idAtivo) REFERENCES Ativos(idAtivo) ON DELETE CASCADE
);

-- Criar tabela Transacao
CREATE TABLE Transacao (
    idTransacao NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idUsuario NUMBER NOT NULL,
    idCarteira NUMBER NOT NULL,
    idAtivo VARCHAR2(255) NOT NULL,
    quantidade NUMBER(10,2) NOT NULL CHECK (quantidade > 0),
    tipo VARCHAR2(10) CHECK (tipo IN ('compra', 'venda')) NOT NULL,
    dataTransacao DATE DEFAULT SYSDATE,
    FOREIGN KEY (idUsuario) REFERENCES Usuario(idUsuario) ON DELETE CASCADE,
    FOREIGN KEY (idCarteira) REFERENCES Carteira(idCarteira) ON DELETE CASCADE,
    FOREIGN KEY (idAtivo) REFERENCES Ativos(idAtivo) ON DELETE CASCADE
);

-- Criar tabela Operacao
CREATE TABLE Operacao (
    idOperacao NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idUsuario NUMBER NOT NULL,
    quantidade NUMBER(10,2) NOT NULL CHECK (quantidade > 0),
    descricao VARCHAR2(255) NOT NULL,
    dataOperacao DATE DEFAULT SYSDATE,
    FOREIGN KEY (idUsuario) REFERENCES Usuario(idUsuario) ON DELETE CASCADE
);
